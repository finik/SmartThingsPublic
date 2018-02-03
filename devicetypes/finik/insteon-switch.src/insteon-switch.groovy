/**
 *  Insteon On/Off Switch
 *  Original Author     : ethomasii@gmail.com
 *  Creation Date       : 2013-12-08
 *
 *  Rewritten by        : idealerror
 *  Last Modified Date  : 2016-12-13
 *
 *  Rewritten by        : kuestess
 *  Last Modified Date  : 2017-12-30
 *  
 *  Rewritten by        : finik
 *  Last Modified Date  : 2018-01-27 
 */
 
import groovy.json.JsonSlurper
 

metadata {
    definition (name: "Insteon Switch", author: "finik", namespace: "finik") {
        capability "Switch Level"
        capability "Polling"
        capability "Switch"
        capability "Refresh"
    }

    // UI tile definitions
    tiles(scale:2) {
       multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.switches.switch.on", backgroundColor:"#79b821", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.switches.switch.off", backgroundColor:"#ffffff", nextState:"turningOn"
            }
            
           tileAttribute ("device.level", key: "SLIDER_CONTROL") {
               attributeState "level", action:"switch level.setLevel"
           }

        }
        
        standardTile("refresh", "device.switch", width: 2, height: 2, inactiveLabel: false, decoration: "flat") {
            state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
        }

        valueTile("level", "device.level", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "level", label:'${currentValue} %', unit:"%", backgroundColor:"#ffffff"
        }

        main(["switch"])
        details(["switch", "level", "refresh"])
    }
}

def on() {
    log.debug "Turning device ON"
    parent.childOn(device.deviceNetworkId)
    sendEvent(name: "switch", value: "on");
    sendEvent(name: "level", value: 100, unit: "%")
}

def off() {
    log.debug "Turning device OFF"
    parent.childOff(device.deviceNetworkId)
    sendEvent(name: "switch", value: "off");
    sendEvent(name: "level", value: 0, unit: "%")
}

def setLevel(level) {
    log.debug "setLevel(${level})"
    if (level > 0) {
        sendEvent(name: "switch", value: "on")
    } else {
        sendEvent(name: "switch", value: "off")
    }
    sendEvent(name: "level", value: level, unit: "%")
    parent.childDim(device.deviceNetworkId, level)
}

def refresh()
{
    log.debug "refresh()"
    parent.childStatus(device.deviceNetworkId)
}

def poll()
{
    log.debug "poll()"
    parent.childStatus(device.deviceNetworkId)
}

def updatestatus(level) {
    log.debug "updatestatus(${device.displayName}, ${level})"
    if (level > 0) {
        sendEvent(name: "switch", value: "on")
    } else {
        sendEvent(name: "switch", value: "off")
    }
    sendEvent(name: "level", value: level, unit: "%")
}

def ping()
{
    log.debug "ping()"
    parent.childStatus(device.deviceNetworkId)
}

def initialize(){
    runEvery5Minutes(poll)
}