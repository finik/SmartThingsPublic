/**
 *  Ring Contact Sensor
 *
 *  Copyright 2018 Dmitry Fink
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
	definition (name: "Ring Contact Sensor", namespace: "finik", author: "Dmitry Fink") {
		capability "Battery"
		capability "Contact Sensor"
        capability "Tamper Alert"
        

	}


	tiles(scale: 2) {
		multiAttributeTile(name:"contact", type: "generic", width: 6, height: 2){
			tileAttribute ("device.contact", key: "PRIMARY_CONTROL") {
				attributeState "closed", 
					label:'closed', 
					icon:"st.contact.contact.closed", 
					backgroundColor:"#00a0dc"
				attributeState "open", 
					label:'open', 
					icon:"st.contact.contact.open", 
					backgroundColor:"#e86d13"	
			}
		}
        
        valueTile("battery", "device.battery", decoration: "flat", width: 2, height: 2){
			state "battery", label:'${currentValue}% \nBattery', unit:""
		}
        
        valueTile("tampering", "device.tamper", width: 2, height: 2) {
			state "detected", label:"Tamper", backgroundColor: "#e86d13"
			state "clear", label:"", backgroundColor: "#ffffff"
		}
		
		main("contact")
		details(["contact", "battery", "tampering"])
	}
}

def updateState(device) {
	log.debug "updateState(${device.id}, ${device.faulted}, ${device.batterLevel})"
	if (device.faulted) {
    	sendEvent(name: "contact", value: "open")
    } else {
    	sendEvent(name: "contact", value: "closed")
    }
    
    if (device.tampered) {
    	sendEvent(name: "tamper", value: "detected")
    } else {
    	sendEvent(name: "tamper", value: "clear")
    }
    
    sendEvent(name: "battery", value: device.batteryLevel)
}

