# Pricehax BT
Prototype of the ESL hack project done by Furrtek (https://github.com/furrtek/PrecIR) using the Pricehax BT Android app, Arduino Nano board and HC-06 (or HC-05) module for Bluetooth communication.

**Disclaimer:** For educational purposes and fun only. Both Furrtek and I decline all responsability for any kind of issues related to an illegal use of this repo. Be smart, the prices in the store's database won't change, you have been warned.

Furrtek's videos of this project are archived in this [playlist](https://www.youtube.com/playlist?list=PLhEz48id1qqD27sRc73mDFfBpu_RcLxfZ) (most in French, auto-translated subtitles are enabled but they are far from perfect).

<img src="PricehaxBT.jpg" width="640" alt="PricehaxBT">

## Build
- Create the dongle on breadboard following the schematic (or order the pcb and solder the components)
- Using the AT commands, change the name of the HC-05/HC-06 module to "PRICEHAX TX V3" and its baudrate to 115200bps
- Program the arduino nano with the included sketch
- Download and install the [Android app](https://github.com/dandri/PricehaxBT/releases/latest), pair the dongle and enjoy ;-)

## Android app changelog

### Version 1.4

Added support for:
- 1628 (B407047372716287)
- 1639 (A406048236716396)
- 1243 Continuum HCN Freezer (F45324224927123434)
- 1627 (D4611412853816278)

### Version 1.3 (21)

**Features**
- Added PP16 IR Protocol support (not full speed, see [Notes](https://github.com/david4599/PricehaxBT#notes)): [speed comparison video](https://youtu.be/DFfLOQh_ERs)
- Updated camera autofocus handling by using continuous focus modes if available
- Improved Bluetooth transmission reliability: the whole data (Pricehax header + PPM frame) is sent to the dongle with a basic checksum instead of using the PPM frame CRC that won't verify the header

**Fixes**
- Fixed bug that was sometimes stopping the image transmission just after the wake-up frame
- Fixed camera preview rotation in reverse landscape and reverse portrait orientations
- Fixed waiting time when sending an image in 2 parts (due to 64kB limit)

### Version 1.2 (20)

**Features**
- Added 1370 and 1371 ESL types (ST HD L Red and ST HD150 Red black housing versions)
- Added checksum verification of the ESL barcode

**Fixes**
- Fixed SmartTag HD T definition in information label

### Version 1.2 (19)

**Features**
- Added a mode to enter the barcode manually

### Version 1.1 (18)

**Features**
- Red (yellow not tested) ESLs are supported: https://youtu.be/0PFMIiDluDw
- Improved Bluetooth transmission reliability: comparison of received checksum and calculated checksum by the dongle
- Added the possibility to stop sending the current image

**Fixes**
- Fixed ST HD150 and ST HD200 definitions

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
- The implemented PP4C and PP16 protocols are not transmitting data at their maximum speeds. In theory, they should be around 10kbps for PP4C and 38kbps for PP16 (in reality, they are measured at 9kbps and 31kbps). Pricehax BT seems to do only 6kbps and 11kbps if my calculations are correct
- I didn't write the app, I just decompiled the sources from the apk (that's why the code is a bit of a mess). Then, I imported them on Android Studio and fixed the decompilation errors preventing the re-compilation
- The app supports Bluetooth communication only, the original communication using audio has been disabled (not the goal of this repo)
- Furrtek did code the Bluetooth feature in the original app, I just made some changes so the feature works with my dongle
- I am not a programming or electronic expert, so the code and the schematic might be better...

#

Copyright (c) Furrtek 2014 & david4599 2019 - 2022
