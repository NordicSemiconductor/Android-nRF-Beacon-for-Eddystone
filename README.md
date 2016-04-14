# Android-nRF-Beacon-for-Eddystone
nRF Beacon for Eddystone is an application that supports the new Eddystone GATT configuration service allowing users to configure your beacon to advertise all types of Eddystone frame types from UID, URL, TLM and the newest EID and eTLM frame types. In addition the application uses Nearby API for scanning Eddystone beacons in close proximity and Google Proximity API to register UID, EID beacons and create attachments for them on the Proximity API cloud.

## Features
The basic features of the application includes

-Foreground and background scanning of beacons using Nearby API which are registered on the Proximity API. Once an EID beacon is registered to the cloud with an attachment, the Nearby API will send the beacon EID packet to the proximity API, resolves the EID packet and retrieves the data attached to it.
 Please note only UID and EID beacon types can be registered on the proximity API
 
-Registering beacons and creating attachments to proximity API

-Configuration of Eddystone beacons using the new Eddystone GATT configuration service.

-URL shortener  for configuring URL beacons

## How to Guide
1. First press the button 1 on the nRF52 Devkit which turns the devkit/beacon in to connectable mode for 60 seconds
2. Press the connect button on the update tab on the application and the list of devices will be prompted.
3. Select the device and you will be challenged with the beacon manufacturer lock code. the lock code used for this application is 16 F's and the application will have the lock code hard coded
4. After entering the correct lock code the application will read through all slots and display the information for each slot.

This application goes hand in hand with the nRF5 SDK for Eddystone posted on Github on the following link. This link also contain a complete how to guide and the new Eddystone GATT configuration specs. Please note that some of the advanced characteristics are not supported on the firmware application and will be implemented in the near future.

https://github.com/NordicSemiconductor/nrf5-sdk-for-eddystone

The nRF Beacon for Eddystone application is available on Playstore on the following link.

https://play.google.com/store/apps/details?id=no.nordicsemi.android.nrfbeacon.nearby 

## Setting up the project
-Two important steps would be to enable the Nearby API, the Proximity API and URL Shortener API from the Google developer console's api manager.

-Generate an API Key for android by clicking in credentials 

-Next using the debug keystore retreive the SHA1 Key and enter it with your project package name

-Next Generate an OAuth 2.0 Client for android and follow the same step as above by entering the SHA1 and the package name

Make sure to enter the API Key in the manifest file

Note:

-Android 4.3 or newer is required.

-Tested on Samsung S3 with Android 4.3, on Nexus 5, 6 and 9 with lollipop & Marshmallow and Samsung Galaxy S6, S7 with Marshmallow.

-Location Services need to be enabled for scanning on android 6.0 Marshmallow requesting a runtime persmission ACCESS_COARSE_LOCATION

-GET_ACCOUNTS permission is required in order to select the account to register and allow access to the Google Proximity API and URL Shortener API.
