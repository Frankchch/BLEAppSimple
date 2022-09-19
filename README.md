# BLEApp
# BLE-simple-app
In this section, we are going to briefly explain the contents of the main Kotlin files. For a more detailed explanation of how the App works, visit [The Ultimate Guide to Android Bluetooth Low Energy](https://punchthrough.com/android-ble-guide/).

## MainActivity

This file contains necessary permisions to use the BLE on your phone, as well as the first screen you see after opening the app. This screen contains a "Escanear" button which starts the search for BLE servers on the vicinities. Said servers are listed with the help of an element called Recycler View. Both the button and the Recycler View are set up on the "onCreate" function:
```
scan_button.setOnClickListener { if (isScanning) stopBleScan() else startBleScan() }
setupRecyclerView()
```
The first one defines which action to perform when clicking on the "Escanear button". The variable "isScanning" informs wether our device is scanning or not and depending on that it starts or stops the search for servers. There is already defined functions for beginning and ending a scan, but these are nested inside 2 custon funcitons to avoid possible errors and to change the status of the variable "isScanning".
To get the scan results, both functions get help from an object "scanCallBack" from the private class ScanCallBack, which uses a function to store the results in a list variable named "scanResults" without repeating detected devices.

The second line call a function to initialize the Recycler View. If we look up this function, we can see it uses a "scan_results_recycler_view" varaiable, note that this is each item of the Recycler View defined in the .xml file linked with the MainActivity. The function needs an adapter and a layoutManager: the adapter is usually an object belonging to a custom class, the layoutManager has already defined options (in this case linear, meaning one column).
```
private fun setupRecyclerView() {
        scan_results_recycler_view.apply {
            adapter = scanResultAdapter
            layoutManager = LinearLayoutManager(
                this@MainActivity,
                RecyclerView.VERTICAL,
                false
            )
            isNestedScrollingEnabled = false
        }
    }
```
The adapter in this case is called "scanResultAdapter", which belongs to the class ScanResultAdapter. This class is defined in the .kt file of the same name and accepts 2 parameters, being the first one a list of ScanResult and the second one a function that activates with a click. The ScanResultAdapter takes the list and uses three functions plus a private class to customize each item acording to the information you wanna show. In the MainActivity file, the "scanResultAdapter" adapter is defined passing the "scanResults" through the class constructor along with a function to connect to the device in which the user clicks.

In this activity we can also find an "onResume" function, in which an object called "conectionEventListener" is set as a listener. This object is designed so it takes us to another activity (screen) when there is a successful connection.

## BleOperationsActivity

This file contains the build up for the second and last screen the app uses. In the "onCreation" function  another Recycler View, but this time for showing the connected device's characteristics. The same function is used in this case with the only change being the adapter, which is now a variable called "characteristicAdapter" from the class "CharacteristicAdapter". This class is similar to the one previously defined, but this time it takes a list of BluetoothCharacteristic as its first parameter. Said parameter is obtained by taking each characteristic of each service and putting them together in a list called "characteristics". The second parameter passed is a function designed to pop up a message containing the options that can be performed in the selected characteristic and then execute them. It is worth noting that for writeable characteristics, the message first needs to be turned into a byte array.

This activity uses a "log" function to write every performed action registered by the listener into a Scroll View element defined in the corresponding .xml file.

## ConnectionEventListener

Here we define the class for listeners, it contains methods that are used alongside the ConnectionManager class to perform the BLE operations.

## ConnectionManager

The class ConnectionManager is here, containts most of the functions related to BLE operations used in the 2 activities. Most of this operations are imported  with the gatt class, but aren't optimized to overcome most common errors. We can find functions which contain imported commands like "gatt.readCharacteristic", "connecGatt" and more, which would throw an error in case we are already connected to a device or there is no connection.

## BleExtensions

This file contains some others functions used in the activities and the ConnectionManager but not directly related to BLE operations. The "findCharacteristic" function is used to locate a device's characteristic based on its uuid so then an operation can be performed in it. The "printProperties" function is used in the BleOperationsActivity for presenting the device's characteristic in the Recycler View. There are also a bunch of small functions who help recognize which properties a characteristic posseses. 
