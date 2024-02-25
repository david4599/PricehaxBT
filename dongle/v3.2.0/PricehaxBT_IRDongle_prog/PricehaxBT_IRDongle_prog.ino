/* 
 *  Pricehax BT IR dongle (for Pricehax Version 1.3+ BT (21+))
 *  furrtek 2014
 *  david4599 2019 - 2024
 */


#define F_CPU 16000000UL
#define NOP __asm__ __volatile__ ("nop")

#define BT_RX_PIN 10
#define BT_TX_PIN 12
#define LED_PIN 2

#define PULSES_PP4C 50
#define PULSES_PP16 26
#define FRAME_MAX_LENGTH 58

// Specific to Pricehax
#define PHX_CODE_PP4C 170
#define PHX_CODE_PP16 171


#include <SoftwareSerial.h>


SoftwareSerial BT(BT_RX_PIN, BT_TX_PIN);

uint8_t gData[FRAME_MAX_LENGTH], gDataLength;
uint16_t gRepeat = 0;
boolean gModePP16 = false, gSendFrame = false, gSentOk = false;

uint8_t gDataTemp[FRAME_MAX_LENGTH], gDataLengthTemp;
uint16_t gRepeatTemp = 0;
boolean gModePP16Temp = false;


// Mandatory header for PP16 protocol, not included in the frame CRC16
const uint8_t gPP16Header[4] = {
    0x00,
    0x00,
    0x00,
    0x40
};

// The timings offsets (especially for PP16) may need to be adjusted again if another uC is used
const uint8_t gOffPP4C = -5;
const uint8_t gOffPP16 = -5;

// PP4C symbols timings
const uint8_t gPP4C[4] = {
     61 + gOffPP4C,
    244 + gOffPP4C,
    122 + gOffPP4C,
    183 + gOffPP4C
};

// PP16 symbols timings
const uint8_t gPP16[16] = {
     27 + gOffPP16,
     51 + gOffPP16,
     35 + gOffPP16,
     43 + gOffPP16,
    147 + gOffPP16,
    123 + gOffPP16,
    139 + gOffPP16,
    131 + gOffPP16,
     83 + gOffPP16,
     59 + gOffPP16,
     75 + gOffPP16,
     67 + gOffPP16,
     91 + gOffPP16,
    115 + gOffPP16,
     99 + gOffPP16,
    107 + gOffPP16
};



// Check the whole Bluetooth data with a basic checksum implementation
boolean isPhxChecksumValid(uint16_t checksumToVerify) {
    uint8_t phxCode = PHX_CODE_PP4C, length = gDataLengthTemp, start = 0;
    uint16_t calcChecksum = 0;

    if (gModePP16Temp) {
        // Skip the PP16 header
        phxCode = PHX_CODE_PP16;
        length = gDataLengthTemp - 4;
        start = 4;
    }

    // Add frame parameters to the checksum
    calcChecksum += phxCode;
    calcChecksum += (uint8_t) (gRepeatTemp >> 8);
    calcChecksum += (uint8_t) (gRepeatTemp & 0xFF);
    calcChecksum += length;
    
    for (uint8_t i = start; i < 4 + length; i++) {
        calcChecksum += gDataTemp[i];
    }

    return calcChecksum == checksumToVerify;
}



/*
// Check the PPM frame CRC16 (replaced by isPhxChecksumValid())
boolean isCRCValid() { 
    uint16_t result = 0x8408, poly = 0x8408, offset = 0;

    // Skip the PP16 header
    if (gModePP16Temp)
        offset = 4;
    
    for (uint16_t i = offset; i < gDataLengthTemp - 2; i++) {
        result ^= gDataTemp[i];
        
        for (uint8_t j = 0; j < 8; j++) {
            if (result & 1) {
                result >>= 1;
                result ^= poly;
            }
            else {
                result >>= 1;
            }
        }
    }

    if (gDataTemp[gDataLengthTemp - 2] != (uint8_t) result)
        return false;
    
    if (gDataTemp[gDataLengthTemp - 1] != (uint8_t) (result >> 8))
        return false;

    return true;
}
*/



// Get a frame from Pricehax sent over Bluetooth
boolean getFrame() {
    uint8_t phxCode, counter;
    uint16_t checksum;
    
    // First byte sent is an ID to avoid unwanted values sent from the Bluetooth module itself
    phxCode = BT.read();
    if (phxCode == PHX_CODE_PP4C || phxCode == PHX_CODE_PP16) {
         counter = 0;

        // Reset the temp data array
        memset(gDataTemp, 0, sizeof(gDataTemp));
        
        while (BT.available()) {
            if (counter == 0) { // Read header data
                gModePP16Temp = false;
                
                // Get the number of times the frame will be transmitted
                gRepeatTemp = (uint8_t) BT.read() << 8;
                gRepeatTemp |= (uint8_t) BT.read();
    
                // Get the data length of the frame
                gDataLengthTemp = (uint8_t) BT.read();

                if (phxCode == PHX_CODE_PP16) {
                    // Add the PP16 header
                    gModePP16Temp = true;
                    memcpy(gDataTemp, gPP16Header, sizeof(gPP16Header));
                    gDataLengthTemp += 4;
                    counter += 4;
                }
            }
            
            if (counter == gDataLengthTemp) {
                // Get the basic Pricehax checksum
                checksum = (uint8_t) BT.read() << 8;
                checksum |= (uint8_t) BT.read();

                while (BT.available()) { // Read unneeded bytes after the end of the frame
                    BT.read();
                }
                
                return isPhxChecksumValid(checksum);
            }
            
            gDataTemp[counter] = (uint8_t) BT.read(); // Get the next byte of the frame
            counter++;
        }
    }

    return false;
}



// Create a burst (40us for PP4C, 21us for PP16) at around 1.25MHz (16MHz/13 = 1.23MHz)
// The number of assembly instructions of the whole loop once compiled is critical
// If the function is changed, the timings will need to be adjusted by adding or removing NOPs
void sendPPMBurst(uint8_t pulses) {
    for(uint8_t i = 0; i < pulses; i++){
        PORTD ^= (1 << LED_PIN);
        NOP;
        NOP;
        PORTD ^= (1 << LED_PIN);
        NOP;
    }
}



// Send a frame using the PP4C protocol
void sendPP4CFrame() {
    uint8_t currentByte;
    uint16_t repeat, symNumber, symCount;

    // Get the number of PP4C symbols the frame contains
    symCount = gDataLength << 2;
    
    for (repeat = 0; repeat < gRepeat; repeat++) {
        cli(); // Stop all interrupts to avoid added delay that will interfere with the PPM signal
        for (symNumber = 0; symNumber < symCount; symNumber++) {

            // Switch to the next byte once the 4 2-bit symbols of the current byte are transmitted
            if ((symNumber & 3) == 0) {
                currentByte = gData[symNumber >> 2];
            }
            
            sendPPMBurst(PULSES_PP4C);
            
            switch(currentByte & 3) {
                case 0:
                    _delay_us(gPP4C[0]);
                    break;
                case 1:
                    _delay_us(gPP4C[1]);
                    break;
                case 2:
                    _delay_us(gPP4C[2]);
                    break;
                case 3:
                    _delay_us(gPP4C[3]);
                    break;
            }

            // Switch to the next symbol
            currentByte >>= 2;
        }
        
        sendPPMBurst(PULSES_PP4C);
        sei(); // Allow interrupts
        
        _delay_ms(2);
    }
}



// Send a frame using the PP16 protocol
void sendPP16Frame() {
    uint8_t currentByte;
    uint16_t repeat, symNumber, symCount;

    // Get the number of PP16 symbols the frame contains
    symCount = gDataLength << 1;
    
    for (repeat = 0; repeat < gRepeat; repeat++) {
        cli(); // Stop all interrupts to avoid added delay that will interfere with the PPM signal
        for (symNumber = 0; symNumber < symCount; symNumber++) {

            // Switch to the next byte once the 2 4-bit symbols of the current byte are transmitted
            if ((symNumber & 1) == 0) {
                currentByte = gData[symNumber >> 1];
            }
            
            sendPPMBurst(PULSES_PP16);
            
            switch(currentByte & 15) {
                case 0:
                    _delay_us(gPP16[0]);
                    break;
                case 1:
                    _delay_us(gPP16[1]);
                    break;
                case 2:
                    _delay_us(gPP16[2]);
                    break;
                case 3:
                    _delay_us(gPP16[3]);
                    break;
                case 4:
                    _delay_us(gPP16[4]);
                    break;
                case 5:
                    _delay_us(gPP16[5]);
                    break;
                case 6:
                    _delay_us(gPP16[6]);
                    break;
                case 7:
                    _delay_us(gPP16[7]);
                    break;
                case 8:
                    _delay_us(gPP16[8]);
                    break;
                case 9:
                    _delay_us(gPP16[9]);
                    break;
                case 10:
                    _delay_us(gPP16[10]);
                    break;
                case 11:
                    _delay_us(gPP16[11]);
                    break;
                case 12:
                    _delay_us(gPP16[12]);
                    break;
                case 13:
                    _delay_us(gPP16[13]);
                    break;
                case 14:
                    _delay_us(gPP16[14]);
                    break;
                case 15:
                    _delay_us(gPP16[15]);
                    break;
            }

            // Switch to the next symbol
            currentByte >>= 4;
        }
        
        sendPPMBurst(PULSES_PP16);
        sei(); // Allow interrupts
        
        _delay_ms(2);
    }
}



void setup() {
    DDRD |= (1 << LED_PIN); // Define led pin as output
    PORTD &= (0 << LED_PIN); // Set led pin to low state
    
    cli(); // Stop all interrupts
    // Set bit TOIE0 in the TIMSK0 register to zero to disable timer0 interrupt
    // This was causing randomly and unwanted 5-6us added delay to _delay_us() due to timer overflow
    TIMSK0 &= ~(1 << TOIE0);
    sei(); // Allow interrupts
    
    BT.begin(115200);
}



void loop() {
    if (BT.available()) {
        if (getFrame()) {
            // Set new PPM frame parameters
            memcpy(gData, gDataTemp, sizeof(gDataTemp));
            gDataLength = gDataLengthTemp;
            gRepeat = gRepeatTemp;
            gModePP16 = gModePP16Temp;
            gSendFrame = true;
            gSentOk = false;
        }
        else {
            // Read all garbage data the Bluetooth module itself may send
            while (BT.available()) {
                BT.read();
            }

            BT.write("1"); // Send an error message to Pricehax if the frame is not properly transmitted
            
            gSendFrame = false;
        }
    }

    if (gSendFrame) {
        if (gModePP16)
            sendPP16Frame();
        else
            sendPP4CFrame();

        if (!gSentOk) {
            BT.write("0"); // Send an OK message to Pricehax once the frame is transmitted
            gSentOk = true;
            gSendFrame = false;
        }

        gDataLengthTemp = 0;
    }
}