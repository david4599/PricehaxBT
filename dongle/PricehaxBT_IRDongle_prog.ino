/* 
 *  Pricehax BT IR dongle (for Pricehax Version 1.1 BT (17.0))
 *  furrtek 2014
 *  david4599 2019
 */

#define F_CPU 16000000L
#define NOP __asm__ __volatile__ ("nop\n\t")

#define BT_RXPIN 10
#define BT_TXPIN 12
#define LEDPIN 2

#include <SoftwareSerial.h>
#include <avr/power.h>
#include <avr/sleep.h>

SoftwareSerial BT(BT_RXPIN, BT_TXPIN);

boolean sendframe = false;
int r, cnt;
uint8_t d;
uint16_t pp, repeat = 0;
byte data[54], datalength;
volatile uint16_t codeptr;
volatile uint8_t dcode[440];



void conversion() { // Convert data to IR format
    codeptr = (uint16_t) datalength * 8; // Get the total number of bits in the frame
    unsigned int j = 0;
    for (unsigned int i = 0; i < codeptr; i += 8) {
        dcode[i]     = ((data[j] & B00000010) >> 1);
        dcode[i + 1] = ((data[j] & B00000001));
        dcode[i + 2] = ((data[j] & B00001000) >> 3);
        dcode[i + 3] = ((data[j] & B00000100) >> 2);
        dcode[i + 4] = ((data[j] & B00100000) >> 5);
        dcode[i + 5] = ((data[j] & B00010000) >> 4);
        dcode[i + 6] = ((data[j] & B10000000) >> 7);
        dcode[i + 7] = ((data[j] & B01000000) >> 6);
        j++;
    }
}



void burst() { // Create 40us burst at around 1.25MHz
    for(int i = 0; i < 50; i++){
        PORTD ^= (1 << LEDPIN);
        NOP;
        NOP;
        PORTD ^= (1 << LEDPIN);
        NOP;
    }
}



void getData() { // Get frames from Pricehax sent over Bluetooth
    if (BT.read() == 170) { // First byte sent is an ID to avoid unwanted values sent from the bluetooth module itself
        cnt = 0;
        
        // Reset data and dcode arrays
        memset(data, 0, sizeof(data));
        memset(dcode, 0, sizeof(dcode));
        
        while (BT.available()) {
            if (cnt == 0) { // Read header data
                // Get the number of times the frame will be transmitted
                repeat = (byte) BT.read() << 8;
                repeat |= (byte) BT.read();
    
                // Get the data length of the frame
                datalength = (byte) BT.read();
            }
            
            if (cnt == datalength) { // Read unneeded bytes after the end of the frame
                while (BT.available()) {
                    BT.read();
                }
                break;
            }
            
            data[cnt] = (byte) BT.read(); // Get the next byte of the frame
            cnt++;
        }
    
        sendframe = true;
    }
}



void IRSend() { // Send a frame following the IR protocol
    for (r = 0; r < (unsigned int) repeat; r++) {
        for (pp = 0; pp < (codeptr - 1); pp += 2) {
            
            burst();
            
            d = ((dcode[pp] << 1) + dcode[pp + 1]);
            switch(d) {
                case 0:
                    _delay_us(56);
                    break;
                    
                case 1:
                    _delay_us(237);
                    break;
                    
                case 2:
                    _delay_us(117);
                    break;
                    
                case 3:
                    _delay_us(178);
                    break;
            }
        }
        
        burst();
        
        _delay_ms(2);
    }
}



void setup() {
    DDRD |= (1 << LEDPIN); // Define pin 2 as output
    PORTD &= (0 << LEDPIN); // Set pin 2 to low state
    
    BT.begin(57600);
}



void loop() {
    if (BT.available()) {
        getData();
    }

    if (sendframe) {
        conversion();
        
        IRSend();
        
        BT.write("ACK"); // Send an answer to Pricehax once the frame is transmitted
        sendframe = false;
    }

    _delay_ms(2);
}
