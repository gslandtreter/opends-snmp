/*
*  This file is part of OpenDS (Open Source Driving Simulator).
*  Copyright (C) 2016 Rafael Math
*
*  OpenDS is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, either version 3 of the License, or
*  (at your option) any later version.
*
*  OpenDS is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU General Public License for more details.
*
*  You should have received a copy of the GNU General Public License
*  along with OpenDS. If not, see <http://www.gnu.org/licenses/>.
*/

package eu.opends.car;

import com.jme3.math.FastMath;

import eu.opends.drivingTask.scenario.ScenarioLoader;
import eu.opends.drivingTask.scenario.ScenarioLoader.CarProperty;
import eu.opends.main.SimulationDefaults;
import eu.opends.main.Simulator;
import eu.opends.tools.PanelCenter;

/**
 * 
 * @author Rafael Math
 */
public class Transmission 
{
	// for speed percentage computation
	private static final float speedAt100PercentMarker = 140f; //FIXME 140f;  --> 80f for trucks
	
	// gears with transmission values
	private static int numberOfGears;
	private static Float[] forwardGears;
	private static float neutralGear;
	private static float reverseGear;
	private static final float wheelCircumference = 380.0f;
	
	// rotation per minute settings
	private static float maxRPM = 0f;//7500f;
	private static float minRPM = 0f;//750f;
	
	private Car car;
	private int gear;
	private Integer rememberGearShiftPosition = null;
	private boolean isAutomaticTransmission;
	private float selectedTransmission;
	private float currentRPM = 0;
	private float previousRPM = 0;
	
	
	public Transmission(Car car)
	{
		this.car = car;
		
		// load settings from driving task file
		ScenarioLoader scenarioLoader = Simulator.getDrivingTask().getScenarioLoader();
		isAutomaticTransmission = true;//scenarioLoader.isAutomaticTransmission(SimulationDefaults.transmission_automatic);
		reverseGear = scenarioLoader.getReverseGear(SimulationDefaults.transmission_reverseGear);
		neutralGear = 0.0f;
		forwardGears = new Float[]{1.281f, 0.678f};//scenarioLoader.getForwardGears(SimulationDefaults.transmission_forwardGears);
		numberOfGears = forwardGears.length;
		minRPM = 0f;//scenarioLoader.getCarProperty(CarProperty.engine_minRPM, SimulationDefaults.engine_minRPM);
		maxRPM = 8000f;//scenarioLoader.getCarProperty(CarProperty.engine_maxRPM, SimulationDefaults.engine_maxRPM);
		
		setGear(1, isAutomaticTransmission, false);
	}
	
	/*
	public float getPowerPercentage(int gear, float currentSpeed)
	{	
		float powerPercentage = 0;
		
		switch (gear)
		{
			case 1  :
				powerPercentage = car.getPowerTrain().getPmax(currentSpeed*forwardGears[0])/300f;
				break;
			case 2  :
				powerPercentage = car.getPowerTrain().getPmax(currentSpeed*forwardGears[1])/300f;
				break;
			case -1 :
				powerPercentage = car.getPowerTrain().getPmax(currentSpeed*reverseGear)/300f;
				break;
			case 0  : powerPercentage = 0; break;
		}

		return Math.min(1.0f, Math.max(0.0f,powerPercentage));
		//return 1f;
	}*/
	
	
	public float getRPMPercentage()
	{
		return Math.min(getRPM()/maxRPM, 1f);
	}
	
	
	public boolean isAutomatic() 
	{
		return isAutomaticTransmission;
	}
	
	
	public void setAutomatic(boolean isAutomatic)
	{
		isAutomaticTransmission = isAutomatic;
		
		if(isAutomatic == false && rememberGearShiftPosition != null)
			gear = rememberGearShiftPosition;
	}

	
	public int getGear()
	{
		return gear;
	}
	
	
	public void updateRPM(float tpf)
	{
		if(gear == 0)
		{
			if(car.getAcceleratorPedalIntensity() > 0)
			{
				//currentRPM = maxRPM;
				currentRPM = car.getAcceleratorPedalIntensity()*maxRPM;
			}
			else
				currentRPM = 0;
		}
		else
		{
			currentRPM = Math.min(Math.abs(car.getCarControl().getCurrentVehicleSpeedKmHour() * 
					selectedTransmission / (wheelCircumference * 0.00006f)), maxRPM);
		}
			
		if(car.isEngineOn())
			currentRPM = Math.max(currentRPM,minRPM);
		
		// do not allow rpm changes of more than 5000 rpm in one second
		float rpmChange = 50000f * tpf;//5000f * tpf;
		if((previousRPM  - currentRPM) > rpmChange)
			currentRPM = previousRPM - rpmChange;
		else if((currentRPM - previousRPM) > rpmChange)
			currentRPM = previousRPM + rpmChange;
		
		previousRPM = currentRPM;
	}
	
	
	public float getRPM()
	{
		return currentRPM;
	}


	public void shiftUp(boolean isAutomatic) 
	{
		setGear(getGear()+1, isAutomatic, false);
	}
	
	
	public void shiftDown(boolean isAutomatic) 
	{
		int gear;
		
		// if automatic transmission --> do not shift down to N and R automatically
		if(isAutomatic)
			gear = Math.max(1, getGear()-1);
		else	
			gear = Math.max(-1, getGear()-1);
		
		setGear(gear, isAutomatic, false);
	}


	public void performAcceleration(float pAccel) 
	{
		float currentEngineSpeed = getRPM();
		float currentVehicleSpeed = FastMath.abs(car.getCarControl().getCurrentVehicleSpeedKmHour());

		int gear = getGear();		
		
		// change gear if necessary (only in automatic mode)
		if(isAutomaticTransmission)
		{
			int bestGear = findBestPowerGear();
			setGear(bestGear, isAutomaticTransmission, false);
		}
		
		// apply power model for selected gear
		//float powerPercentage = getPowerPercentage(gear, speedPercentage);
		
		// cap if max speed was reached
		//float limitedSpeed = car.getMaxSpeed();
		//if((currentVehicleSpeed >= limitedSpeed-1))
		//	powerPercentage = powerPercentage * (limitedSpeed - currentVehicleSpeed);
		
		// accelerate
		//car.getCarControl().accelerate(pAccel * powerPercentage * Math.signum(gear));
		pAccel = pAccel * Math.signum(gear);
		car.getCarControl().accelerate(pAccel);

		// output texts
		PanelCenter.setGearIndicator(gear, isAutomaticTransmission);
		PanelCenter.getEngineSpeedText().setText((int) currentEngineSpeed + " rpm");
		//TextCenter.getEngineSpeedText().setText((int) (powerPercentage * 100) + " %");
	}
	
	
	public void setGear(int gear, boolean isAutomatic, boolean rememberGear)
	{
		isAutomaticTransmission = isAutomatic;
		
		if(rememberGear)
			rememberGearShiftPosition = gear;
		
		this.gear = Math.min(numberOfGears, Math.max(-1,gear));
		
		switch (this.gear)
		{
			case -1 : selectedTransmission = reverseGear; break;
			case 0  : selectedTransmission = neutralGear; break;
			default : selectedTransmission = forwardGears[this.gear-1]; break;
		}
	}

	private int findBestPowerGear()
	{
		if (gear == 1){
			if (currentRPM > 7000f)	return 2;
			else return 1;
		}
		else{
			if (currentRPM < 2000f)	return 1;
			else return 2;
		}
	}

	public float getMinRPM() 
	{
		return minRPM;
	}
}
