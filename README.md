# Pricehax BT
Prototype of the ESL hack project done by Furrtek (https://github.com/furrtek/PrecIR) using the Pricehax BT Android app, Arduino Nano board and HC-06 (or HC-05) module for Bluetooth communication.

**Disclaimer:** Only for educational purposes and fun. Both Furrtek and I decline all responsability for any kind of issues related to an illegal use of this repo. Be smart, the prices in the store's database will not change, you have been warned.

Videos about this project are archived in this [playlist](https://www.youtube.com/playlist?list=PLhEz48id1qqD27sRc73mDFfBpu_RcLxfZ) (most in French).

<img src="PricehaxBT.jpg" width="640" alt="PricehaxBT">

## Build
- Create the dongle on breadboard following the schematic (or order the pcb and solder the components)
- Using the AT commands, change the name of the HC-05/HC-06 module to "PRICEHAX TX V3" and its baudrate to 57600bps (115200bps and more will not work because of the SoftwareSerial library usage that produces errors at those rates)
- Program the arduino nano with the included sketch
- Download and install the [Android app](https://github.com/david4599/PricehaxBT/releases/latest), pair the dongle and enjoy ;-)

## Android app changelog
### Version 1.2 (19)

**Features**
- Added a mode to enter the barcode manually

### Version 1.1 (18)

**Features**
- Red (yellow not tested) ESLs are supported: https://youtu.be/0PFMIiDluDw
- Improved Bluetooth transmission reliability: comparison of received checksum and calculated checksum by the dongle
- Added the possibility to stop sending the current image

**Fixes**
- Fixed ST HD150 and ST HD200 definition

### Version 1.1 (17)

**Features**
- Added the ability to scroll if pages are greater than the height of the screen (especially in landscape mode)
- Display debug infos for 24h
- Hide debug infos feature added
- Ability to blink the green LED on ST ESLs (not working yet on some): https://youtu.be/b0Rn40alxQg
- Start autofocus by touching the preview screen on "PLID Scan" tab (not working sometimes?)
- ESL types added (mainly graphic ESLs, not tested on the most of them but it should work)
- Automatically choose sending compressed or raw data to graphic ESLs
- Ability to force sending uncompressed data to graphic ESLs
- The number of frames repeats for graphic ESLs can be chosen (speed transmission vs reliability)
- The dongle can be manually connected or disconnected in "Config" tab

**Fixes**
- Fixed some bugs and app crashes

## Notes
- I didn't write the app, I only decompiled the sources from the apk (that's why the code is a bit of a mess). Then, I imported them on Android Studio and fixed the decompilation errors preventing the re-compilation
- The app supports only Bluetooth communication, the original communication using audio has been disabled (not the goal of this repo)
- Furrtek did code the Bluetooth feature in the original app, I just made some changes so the feature works with my dongle
- I am not a programming or electronic expert, so the code and the schematic might be better...

#

Copyright (c) Furrtek 2014 & david4599 2019 - 2022