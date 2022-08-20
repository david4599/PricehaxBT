/* 
 *  Pricehax BT IR dongle (for Pricehax Version 1.1 BT (18.0))
 *  furrtek 2014
 *  david4599 2019
 */

#define F_CPU 16000000L
#define NOP __asm__ __volatile__ ("nop\n\t")

#define BT_RXPIN 10
#define BT_TXPIN 12
#define LEDPIN 2

#include <SoftwareSerial.h>

SoftwareSerial BT(BT_RXPIN, BT_TXPIN);

boolean sendframe = false;
uint8_t b, cnt, data[54], datalength;
uint16_t s, sym_count, r, repeat = 0, result, poly;



void CRCCalc() {
    result = 0x8408;
    poly = 0x8408;
    
    for (int i = 0; i < datalength-2; i++) {
        result ^= data[i];
        
        for (int j = 0; j < 8; j++) {
            if (result & 1) {
                result >>= 1;
                result ^= poly;
            }
            else {
                result >>= 1;
            }
        }
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
        
        // Reset data array
        memset(data, 0, sizeof(data));
        
        _delay_ms(1);
        
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
    sym_count = datalength << 2;
    
    for (r = 0; r < (unsigned int) repeat; r++) {
        for (s = 0; s < sym_count; s++) {
            if ((s & 3) == 0) {
                b = data[s >> 2];
            }
            
            burst();
            
            switch(b & 3) {
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

            b >>= 2;
        }
        
        burst();
        
        _delay_ms(2);
    }
}



void setup() {
    DDRD |= (1 << LEDPIN); // Define led pin as output
    PORTD &= (0 << LEDPIN); // Set led pin to low state
    
    BT.begin(57600);
}



void loop() {
    if (BT.available()) {
        getData();
        CRCCalc();
        
        if (data[datalength-2] != (uint8_t) result || data[datalength-1] != (uint8_t) (result >> 8)) {
            BT.write("1");
            sendframe = false;
        }
    }

    if (sendframe) {
        IRSend();
        
        BT.write("0"); // Send an answer to Pricehax once the frame is transmitted
        sendframe = false;
    }

    _delay_ms(1);
}