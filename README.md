# Pricehax BT
Wireless prototype based on Arduino Nano + HC-06 (or HC-05) Bluetooth module implementing [Furrtek's ESL reverse engineering project](http://furrtek.free.fr/index.php?a=esl).

<img src="PricehaxBT.jpg" width="640" alt="PricehaxBT">

This tool receives ESL data from the included PricehaxBT Android app. It is a decompiled (see [Notes](https://github.com/david4599/PricehaxBT#notes)) and updated version of Pricehax, an old app originally made by Furrtek which was using audio to transmit data to a homemade dongle.

For archival sake, Furrtek's videos of the whole research process are backed up in this [playlist](https://www.youtube.com/playlist?list=PLhEz48id1qqD27sRc73mDFfBpu_RcLxfZ) (most in French, auto-translated subtitles are available but they are far from perfect).

**For educational purposes and fun only.** Both Furrtek and I decline all responsability for any kind of issues related to an illegal use of this repo. Be smart, the prices in the store's database won't change, you have been warned.

_Update:_ Furrtek recently reworked and updated the app to communicate with his ESL Blaster dongle. Check out his version on the [PrecIR repo](https://github.com/furrtek/PrecIR).

## Build
- Create the dongle on breadboard by following the schematic (or order the pcb and solder the components)
- Configure the HC-05/HC-06 module using the AT commands to change its name to "PRICEHAX TX V3" (without the quotes) and its baudrate to 115200bps
- Program the Arduino Nano with the included sketch
- Download and install the [Android app](https://github.com/david4599/PricehaxBT/releases/latest)
- Pair the dongle in the phone settings and go to the app. The HC-05/HC-06 module should connect automatically, its LED should stay on and not blink anymore (scroll the tabs list and go to the config tab for manual connection/disconnection)
- Enjoy ;-)

## Android app changelog

### Version 1.4 (22)

**Features**

- Added a few ESL types

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
- The implemented PP4C and PP16 protocols are not transmitting data at their maximum speeds. In theory, they should be around 10kbps for PP4C and 38kbps for PP16. Pricehax BT seems to do only 6kbps and 11kbps if my calculations are correct
- I didn't write the app, I simply decompiled the original Pricehax apk with an online apk decompiler website. Then, I imported the sources on Android Studio and fixed the decompilation errors preventing the re-compilation
- The app supports Bluetooth only, the communication using audio has been disabled (not the goal of this repo)
- Furrtek did code the Bluetooth feature in the original app, I just made some changes so the feature works with my dongle
- I am not a programming or electronic expert, so the code and the schematic might be better...

#

Copyright (c) Furrtek 2014 & david4599 2019 - 2024
