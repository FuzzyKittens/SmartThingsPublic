/**
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
    definition (name: "Springs Window Fashions Remote Kustom", namespace: "smartthings", author: "SmartThings", ocfDeviceType: "x.com.st.d.remotecontroller", hidden: true) {

        capability "Battery"
        capability "Actuator"
		capability "Button"
		capability "Configuration"
		capability "Sensor"

		attribute "numberOfButtons", "number"
        attribute "numButtons", "string"
        attribute "buttonOneLastActivity", "string"
        attribute "buttonTwoLastActivity", "string"

        fingerprint mfr:"026E", prod:"5643", model:"5A31", deviceJoinName: "2 Button Window Remote"
        fingerprint mfr:"026E", prod:"4252", model:"5A31", deviceJoinName: "3 Button Window Remote"
    }

    simulator {

    }

    tiles {
        standardTile("state", "device.state", width: 2, height: 2) {
            state 'connected', icon: "st.unknown.zwave.remote-controller", backgroundColor:"#ffffff"
        }

        valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
            state "battery", label:'batt.', unit:"",
                    backgroundColors:[
                            [value: 0, color: "#bc2323"],
                            [value: 6, color: "#44b621"]
                    ]
        }

        valueTile("ButtonOne", "device.button1", width: 6, height: 2) {
        	state "default", label: 'Button 1 was ${currentValue}.'
        }        

        valueTile("ButtonOneActivity", "device.buttonOneLastActivity", width: 6, height: 2) {
        	state "default", label: '${currentValue}'
        }     

        valueTile("ButtonTwo", "device.button2", width: 6, height: 2) {
        	state "default", label: 'Button 2 was ${currentValue}.'
        }

        valueTile("ButtonTwoActivity", "device.buttonTwoLastActivity", width: 6, height: 2) {
        	state "default", label: '${currentValue}'
        }
        
        valueTile("NumberOfButtons", "device.numberOfButtons", width: 6, height: 2) {
        	state "default", label: 'This remote has ${currentValue} buttons.'
        }
        
        main "state", "ButtonOne", "ButtonOneActivity", "ButtonTwo", "ButtonTwoActivity", "NumberOfButtons"
        details(["state", "battery", "ButtonOne", "ButtonOneActivity", "ButtonTwo", "ButtonTwoActivity", "NumberOfButtons"])
    }

}

def installed() {
    if (zwaveInfo.zw && zwaveInfo.zw.cc?.contains("84")) {
        response(zwave.wakeUpV1.wakeUpNoMoreInformation())
    }
}

def updated() {
	log("Device Updated.", "INFO")
    initialization()
}

def initialization() {
	log("Log level selected = ${logging}.", "INFO")
    log("Number of Buttons = 2.", "INFO")
    sendEvent(name: "numberOfButtons", value: 2)
    sendEvent(name: "numButtons", value: "2")
}

def buttonEvent(buttonNumber, buttonAction) {
	def button = buttonNumber as Integer
    switch(button) {
    	case 1:
        	updateButtonOneLastActivity(new Date())
            break
            
        case 2:
        	updateButtonTwoLastActivity(new Date())
            break
            
        default:
        	break
    }

	switch(buttonAction) {
    	case "pushed":
        	log("Button #${button} was pushed.", "INFO")
            createEvent(name: "button", value: "pushed", data: [buttonNumber: button], source: "DEVICE", descriptionText: "$device.displayName button $button was pushed", isStateChange: true)
            break
            
    	case "held":
        	log("Button #${button} was held.", "INFO")
            createEvent(name: "button", value: "held", data: [buttonNumber: button], source: "DEVICE", descriptionText: "$device.displayName button $button was held", isStateChange: true)
            break

        case "released":
        	log("Button #${button} was released.", "INFO")
            createEvent(name: "button", value: "released", data: [buttonNumber: button], source: "DEVICE", descriptionText: "$device.displayName button $button was released", isStateChange: true)
            break

        default:
        	log("Unknown Event on Button #${button}.", "WARN")
    }
}

def determinePress(button, action) {
    switch(action) {
    	case "0":
        	log("Button ${button} Pressed", "DEBUG")
            buttonEvent(button, "pushed")
            break

        case "1":
        	log("Button ${button} Held and Released", "DEBUG")
            buttonEvent(button, "released")
            break

        case "2":
        	log("Button ${button} Held", "TRACE")
            buttonEvent(button, "held")
            break

        default:
        	log("Unknown Press on Button ${button}", "WARN")
    }  
}

def updateButtonOneLastActivity(lastActivity) {
	def finalString = lastActivity?.format('MM/d/yyyy hh:mm a',location.timeZone)    
	sendEvent(name: "buttonOneLastActivity", value: finalString, source: "DEVICE")
}

def updateButtonTwoLastActivity(lastActivity) {
	def finalString = lastActivity?.format('MM/d/yyyy hh:mm a',location.timeZone)    
	sendEvent(name: "buttonTwoLastActivity", value: finalString, source: "DEVICE")
}

def parse(String description) {
    def result = null
    if (description.startsWith("Err")) {
        if (description.startsWith("Err 106") && !state.sec) {
            state.sec = 0
        }
        result = createEvent(descriptionText:description, displayed:true)
    } else {
        def cmd = zwave.parse(description)
        if (cmd) {
            result = zwaveEvent(cmd)
        }
    }
    return result
}

def zwaveEvent(physicalgraph.zwave.commands.wakeupv1.WakeUpNotification cmd) {
    def result = []
    result << createEvent(descriptionText: "${device.displayName} woke up", isStateChange: true)
    result << response(zwave.wakeUpV1.wakeUpNoMoreInformation())
    result
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    state.sec = 1
    createEvent(isStateChange: true, descriptionText: "$device.displayName: ${cmd.encapsulatedCommand()} [secure]")
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd) {
    createEvent(isStateChange: true, descriptionText: "$device.displayName: ${cmd.encapsulatedCommand()}")
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
    createEvent(isStateChange: true, "$device.displayName: $cmd")
}

def zwaveEvent(physicalgraph.zwave.commands.batteryv1.BatteryReport cmd) {
    def map = [ name: "battery", unit: "%" ]
    if (cmd.batteryLevel == 0xFF) {
        map.value = 1
        map.descriptionText = "${device.displayName} has a low battery"
        map.isStateChange = true
    } else {
        map.value = cmd.batteryLevel
    }
    state.lastbatt = now()
    createEvent(map)
}

private command(physicalgraph.zwave.Command cmd) {
    if (deviceIsSecure) {
        zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
    } else {
        cmd.format()
    }
}

private getDeviceIsSecure() {
    if (zwaveInfo && zwaveInfo.zw) {
        return zwaveInfo.zw.endsWith("s")
    } else {
        return state.sec ? true : false
    }
}