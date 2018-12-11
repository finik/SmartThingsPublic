/**
 *  Ring Alarm
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
	definition (name: "Ring Alarm", namespace: "finik", author: "Dmitry Fink") {
		capability "Alarm System"
        attribute "status", "string"
	}

	tiles {
        multiAttributeTile(name:"status", type: "generic", width: 6, height: 4){
            tileAttribute ("device.status", key: "PRIMARY_CONTROL") {
                attributeState "off", label:'${name}', icon: "st.security.alarm.off", backgroundColor: "#1998d5"
                attributeState "stay", label:'${name}', icon: "st.Home.home4", backgroundColor: "#e58435"
                attributeState "away", label:'${name}', icon: "st.security.alarm.on", backgroundColor: "#e53935"
            }
        }	
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'alarmSystemStatus' attribute

}

def updateState(String status) {
	log.debug "updateState '${status}'"
	sendEvent(name: "status", value: status)
	//def currentState = location.currentState("alarmSystemStatus")
	//log.debug "current alarm state: $currentState"
	sendEvent(name: "alarmSystemStatus", value: status)
}
