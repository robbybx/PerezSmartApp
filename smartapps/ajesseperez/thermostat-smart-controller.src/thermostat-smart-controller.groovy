/**
 *  Thermostat Smart Controller
 *
 *  Copyright 2017 Jesse Perez
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
definition(
    name: "Thermostat Smart Controller",
    namespace: "ajesseperez",
    author: "Jesse Perez",
    description: "Auto Thermostate",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section(){
        input "thermostat", "capability.thermostat", title: "Which Thermostat?", required: true
        input "tempsen", "capability.temperatureMeasurement", title: "Use Which Temperature Sensor", required: true
    }
    section(hidden: true, "Fan Control"){
        input "fanon", "number", title: "How long to has the fan on?", required: false
        input "fanoff", "number", title: "How long to has the fan off?", required: false
    }
    section(hidden: true, "Comfort Settings"){
        input "comfort_modes", "mode", title: "Select Comfort Mode(s)", required: false, multiple: true
        input "comfort_high", "decimal", title: "Set Comfort High Temp", required: false
        input "comfort_low", "decimal", title: "Set Comfort Low Temp", required: false
    }
    section(hidden: true, "Semi-Comfort Settings"){
        input "semicomfort_modes", "mode", title: "Select Semi-Comfort Mode(s)", required: false, multiple: true
        input "semicomfort_high", "decimal", title: "Set Semi-Comfort High Temp", required: false
        input "semicomfort_low", "decimal", title: "Set Semi-Comfort Low Temp", required: false
    }
    section(hidden: true, "Night Settings"){
        input "night_modes", "mode", title: "Select Night Mode(s)", required: false, multiple: true
        input "night_high", "decimal", title: "Set Night High Temp", required: false
        input "night_low", "decimal", title: "Set Night Low Temp", required: false
    }
}

def installed() {
   log.debug "Installed with settings: ${settings}"
   initialized()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
    initialized()
    intCheck()
}

//Schedule setpointCheck to run every 10Min and to run Initial Checks when started DONE!
def initialized() {
    subscribe(tempsen, "temperature", temperaturehandler)
    subscribe(location, "mode", modeChangeHandler)
    subscribe(thermostat, "thermostatfanmode", thermostatfanmodehandler)
    subscribe(thermostat, "thermostatmode", thermostatmodehandler)
    runEvery10Minutes(setpointCheck)
    intCheck()
}

//Initial Checks DONE!
def intCheck() {
	def tempState = tempsen.currentTemperature
    def thermodeState = thermostat.currentState("thermostatMode")
    def therfanmodeState = thermostat.currentState("thermostatFanMode")
    //log.debug "Temperature is ${tempState.value} Degrees"
    //log.debug "Thermostat Mode is ${thermodeState.value}"
    //log.debug "Thermostat Fan Mode is ${therfanmodeState.value}"
    
    if (location.mode in comfort_modes) {
    	log.info "Switching to Comfort Mode."
        state.comfortLVL = "Comfort Mode"
    }
    else if (location.mode in semicomfort_modes) {
    	log.info "Switching to Semi-Comfort Mode."
        state.comfortLVL = "Semi-Comfort Mode"
    }
    else if (location.mode in night_modes) {
    	log.info "Switching to Night Mode."
        state.comfortLVL = "Night Mode"
    }
    else {
    	log.error "WTF! $evt Mode not setup."
        state.comfortLVL = "NULL Mode"
    }
}

//This sets the Comfort Mode BASED on SMARTTHINGS MODE. DONE! 
def modeChangeHandler(evt){
	log.debug "modeChangeHandler called: $evt.value"
    
    if (location.mode in comfort_modes) {
    	log.info "Switching to Comfort Mode."
        state.comfortLVL = "Comfort Mode"
    }
    else if (location.mode in semicomfort_modes) {
    	log.info "Switching to Semi-Comfort Mode."
        state.comfortLVL = "Semi-Comfort Mode"
    }
    else if (location.mode in night_modes) {
    	log.info "Switching to Night Mode."
        state.comfortLVL = "Night Mode"
    }
    else {
    	log.error "WTF! $evt Mode not setup."
        state.comfortLVL = "NULL Mode"
    }
}

//Turns off thermostate if within desired settings
def temperaturehandler(evt){
	def tempState = tempsen.currentTemperature
    log.debug "Temperature is ${tempState.value} Degrees"
    
        if (tempState > comfort_high) {
        	log.debug "Temperature is Higher Than Desired for Comfort Settings"
        }
        else if (tempState < comfort_low) {
        	log.debug "Temperature is Lower Than Desired for Comfort Settings"
        }
        else {
        	log.debug "Temperature is within Desired Comfort Settings"
        }
}

//Monitors Mode of Thermostate, When Change occurs it checks setpoint after 30 sec. DONE!
def thermostatmodehandler(evt){
	def thermodeState = thermostat.currentState("thermostatMode")
	log.debug "Thermostat Mode is ${thermodeState.value}"
    runIn(30,setpointCheck)
}

//This sets the thermostat set points, Skips if Thermostat is off
def setpointCheck(){
    
    if (thermostat.currentState("thermostatMode") == off) {
    	log.debug "Thermostat is off. Not Verifying anything."
    }
    else if (state.comfortLVL == "Comfort Mode"){
    	log.debug "Verifying Comfort Setpoints"
        
		thermostat.setCoolingSetpoint(comfort_high)
        thermostat.setHeatingSetpoint(comfort_low)
        log.debug "Setting Cooling Setpoint to $comfort_high and Heating Setpoint to $comfort_low"
    }
    else if (state.comfortLVL == "Semi-Comfort Mode") {
    	log.debug "Verifying Semi-Comfort Setpoints"
        
        thermostat.setCoolingSetpoint(semicomfort_high)
        thermostat.setHeatingSetpoint(semicomfort_low)
        log.debug "Setting Cooling Setpoint to $semicomfort_high and Heating Setpoint to $semicomfort_low"
    }
    else if (state.comfortLVL == "Night Mode") {
    	log.debug "Verifying Night Setpoints"
        
        thermostat.setCoolingSetpoint(night_high)
        thermostat.setHeatingSetpoint(night_low)
        log.debug "Setting Cooling Setpoint to $night_high and Heating Setpoint to $night_low"
    }
    else {
    	log.debug "Hmmm... No Setpoints to Verify"
    }
}

//
//FanStuff
//
def thermostatfanmodehandler(evt){
	log.debug "FanMode Event Occured"
	if (thermostat.thermostatFanMode == auto) {
    	state.fanMode = "Auto Mode"
        runIn(fanon * 60, fanSwitchOn)
    }
    else if (thermostat.thermostatFanMode == on) {
    	state.fanMode = "On Mode"
        runIn(fanoff * 60, fanSwitchOff)
    }
}

def fanSwichOn(){
	log.debug "Fan Switcher was ran ON."
}

def fanSwichauto(){
	log.debug "Fan Switcher was ran AUTO."
}