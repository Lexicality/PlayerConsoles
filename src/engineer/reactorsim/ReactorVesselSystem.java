package engineer.reactorsim;

import java.awt.Rectangle;
import java.util.ArrayList;

import processing.core.PApplet;
import processing.core.PVector;
import common.ConsoleLogger;
import common.HardwareEvent;
import engineer.reactorsim.ReactorManager.ReactorCheck;
import engineer.reactorsim.ReactorSystem.ReactorResource;

/* simulate the actual reactor
 * reactor takes fuel, turns it into plasma and generates steam
 * PLASMA:
 * plasma moves around the vessel when ship is damaged or bumped, plasma can heat the walls and break reactor
 * coil field modes can move it left and right in the vessel to prevent it hitting walls
 * 
 * FUEL:
 * fuel is drawn from the fuel system (D/T dials). Rate controls size and awkwardness of plasma
 * 
 * VESSEL:
 * produces steam, this is consumed by the turbines
 */
public class ReactorVesselSystem extends ReactorSystem {
	
	float plasmaPos = 100;
	float plasmaDelta = 0f;
	float plasmaDamageDelta = 0f;
	float plasmaSize = 0f;
	int[] coilMode = new int[2];	//polarity of field coils
	boolean reactorRunning = false;

	public ReactorVesselSystem() {
		name = "Reactor Vessel";
		// configure resources for reactor
		ReactorResource steamResource = new ReactorResource();
		steamResource.typeTag = "STEAM";
		resourceStore.put("STEAM", steamResource);
		
		ReactorResource heatResource = new ReactorResource();
		heatResource.typeTag = "HEAT";
		resourceStore.put("HEAT", heatResource);
		
		ReactorResource coolant = new ReactorResource();
		coolant.typeTag = "COOLANT";
		resourceStore.put("COOLANT", coolant);
		
		ReactorResource structure = new ReactorResource();
		structure.typeTag = "STRUCTURE";
		structure.setAmount(100f);
		resourceStore.put("STRUCTURE", structure);
		
		coilMode[0] = 1;
		coilMode[1] = 1;
	
	}
	
	@Override
	public void tick(){
		//now draw some coolant off of the valves. Consume more if valves are on
		CoolantValveSystem coolantSys = (CoolantValveSystem) inboundConnections.get("Coolant valve");
		if(coolantSys != null){
			float toConsume = 5f;
			float coolantAmount = coolantSys.consumeResource("COOLANT", toConsume);
			resourceStore.get("COOLANT").change(coolantAmount);
		}
		
		if(reactorRunning){
			
			//consume some fuel and grow plasma by some amount
			FuelTankSystem fuelTanks = (FuelTankSystem) inboundConnections.get("Fuel Tank");
			float fuel = fuelTanks.consumeResource("FUEL", 1f);
		//	ConsoleLogger.log(this, "consumed : " + fuel);
			
			float targetSize = (fuel / 1f) * 2f;
			//ConsoleLogger.log(this, "targets zies: " + targetSize);
			plasmaSize = PApplet.lerp(plasmaSize, targetSize, 0.1f);
			if(plasmaSize <= 0.1f){
				stopReactor();
			} else if (plasmaSize > 2.5){
				plasmaSize = 2.5f;
			}
			
			
			
			//update the plasma position in the vessel
			//calculate plasma delta, based on size, coil modes		
			float sizeModifier = plasmaSize / 0.5f;		//larger plasmas should be harder to control
			if(coilMode[0] != coilMode[1]){
				if(coilMode[0] == 0){
					plasmaDelta = -.06f * sizeModifier;
				} else if (coilMode[0] == 1){
					plasmaDelta = .06f * sizeModifier;
				}
			} else {
				plasmaDelta = 0f;
				
			}
			//tend plasma damage delta toward zero
			if(plasmaDamageDelta <= -0.1f){
				plasmaDamageDelta += 0.005f;
				ConsoleLogger.log(this, "elt " + plasmaDamageDelta);
			} else if (plasmaDamageDelta >= 0.1f){
				plasmaDamageDelta -= 0.005f;
				ConsoleLogger.log(this, "elt " + plasmaDamageDelta);
			} else {
				plasmaDamageDelta = 0f;
			}
			
			
			plasmaPos += (plasmaDamageDelta + plasmaDelta) * plasmaSize;	//scale with size, implying that larger ones will move quicker
			
			
			
			//test for wall collisions and heat the reactor up if plasma is touching it
			float warmAmount = 0;
			if(plasmaPos <= 0){
				plasmaPos = 0;
				warmAmount = 0.5f * plasmaSize;
			} else if (plasmaPos >= 200){
				plasmaPos = 200;
				warmAmount = 0.5f * plasmaSize;
			} else {
				warmAmount = 0.19f * plasmaSize;	//slowly warm the reactor anyway
													//with all 3 valves open this slowly creeps up
													//to prevent players running reactor at full power all the time

			}
			warmAmount += 0.1f;	//just heat up for being turned on
			resourceStore.get("HEAT").change(warmAmount);
			
			//damage the reactor based on the heat overload
			float heatLevel = resourceStore.get("HEAT").getAmount();
			if(heatLevel >= 90){
				float damgAmount = heatLevel - 90;
				resourceStore.get("STRUCTURE").change(-damgAmount * 0.005f);
			}
			
			
			//power generated is based on the size of the plasma. If it hits the walls then reduce its size
			//and heat the reactop up
			
			
				
				//trade some of the coolant in the reactor for a bit of heat
			float amt = consumeResource("COOLANT", 0.4f);
			resourceStore.get("HEAT").change(-amt);
			
			
			//generate steam based on plasma size
			
			resourceStore.get("STEAM").change(plasmaSize * 2.0f);
			
			} else {
				plasmaSize = 0f;
			}
		
	}

	@Override
	public void setScreenPosition(PVector pos){
		this.screenPosition = pos;
		bounds = new Rectangle((int)screenPosition.x, (int)screenPosition.y, 200,200);

	}
	
	@Override
	public float consumeResource(String resName, float amount) {
		ReactorResource res = resourceStore.get(resName);
		if(res == null){
			return 0;
		}
		
		float amt = res.getAmount() - amount;
		if(amt < 0 ){
			amt = amount + amt;
			//in this case may want to punish this system for trying to draw too much
			//of a resource
		} else {
			amt = amount;
		}
		res.change(-amount);
		

		return amt;
		
	}

	@Override
	public void controlSignal(HardwareEvent e) {
		// TODO Auto-generated method stub
		if(e.event == "MOUSECLICK"){
			int mx = e.value >> 16;
			int my = e.value & 65535;
			if (mx < screenPosition.x + 20){
				coilMode[0] = 1-coilMode[0];
			} else if (mx > screenPosition.x + bounds.width - 20){
				coilMode[1] = 1-coilMode[1];
			} else {
				if(reactorRunning == false){
					startReactor();
				} else {
					stopReactor();
				}
			}
		}

	}

	private void stopReactor() {
		// TODO Auto-generated method stub
		reactorRunning = false;
		plasmaSize = 0f;
	}

	private void startReactor() {
		reactorRunning = true;
		plasmaSize = 1.1f;
	}

	@Override
	public void draw(PApplet context) {
		context.pushMatrix();
		context.translate(screenPosition.x, screenPosition.y);
		context.noFill();
		context.rect(0,0, 200,200);
		//optimal area
		context.rect(50,50, 100, 100);
		
		//field coils
		String t = "";
		for(int i = 0; i < 2; i++){
			if(coilMode[i] == 0){
				context.fill(0,0,255);
				t = "+";
				
			} else {
				context.fill(255,0,0);
				t = "-";
			}
			context.rect(i * 200  -10, 20, 20, 150);
			context.fill(255);;
			context.text(t, i * 200, -20);
		}
		
		
		//if reactor is running then draw the plasma
		if(reactorRunning){
			context.fill(255);
			//draw plasma
			float offsetX = (float)Math.sin(System.currentTimeMillis()/50 ) * 3;
			float offsetY = (float)Math.sin(System.currentTimeMillis()/ 80 ) * 3;
			context.ellipse(plasmaPos + offsetX, 75 + offsetY, 14 * plasmaSize,50 * plasmaSize);
			context.text(plasmaSize, plasmaPos, 120);
		} else {
			context.text("NO PLASMA", 80,120);
		}
		
		
		context.fill(255);
		context.translate(0, 140);
		drawResources(context);
		
		context.popMatrix();
		
	}

	@Override
	public void applyDamage(float amount) {
		plasmaDamageDelta = PApplet.map((float)Math.random(), 0, 1, -0.9f, 0.9f);
		plasmaDamageDelta *= plasmaSize * 0.2f;
	}

	@Override
	public ArrayList<ReactorCheck> checkForProblems() {
		ArrayList<ReactorCheck> returnProblems = new ArrayList<ReactorCheck>();
		//current checks
		//1. check plasma pos and movement rate, suggest altering polarity
		//2. suggest plasma size as too small/large (based on coolant levels?)
		//3. low structure warnings (tell them to engage internal repairs
		
		//check if plasma is near the walls
		ReactorCheck movementCheck = new ReactorCheck("PLASMA_POSITION", false);
		if(plasmaPos < 50){
			movementCheck.setMessage("PLASMA OUTSIDE SAFE AREA, SWITCH COILS TO - and +");
		} else if (plasmaPos > 150){
			movementCheck.setMessage("PLASMA OUTSIDE SAFE AREA, SWITCH COILS TO + and -");
		} else {
			movementCheck.isOk = true;
		}
		returnProblems.add(movementCheck);
		
		//check were in the safe area, if so and the plasma is still moving then add a warning to stop it
		ReactorCheck positionCheck = new ReactorCheck("PLASMA_SAFE", false);
		if(plasmaPos >= 50 && plasmaPos <= 150){
			if(plasmaDelta >= -0.01f && plasmaDelta <= 0.01f){
				positionCheck.isOk = true;
			} else {
				positionCheck.setMessage("PLASMA IN SAFE ZONE, SET COILS TO + / +");
			}
		}
		returnProblems.add(positionCheck);
		
		//check the plasma size, suggest larger or smaller (1 is a good figure)
		ReactorCheck sizeCheck = new ReactorCheck("PLASMA_SIZE", false);
		if(plasmaSize < 0.8f){
			sizeCheck.setMessage("PLASMA TOO SMALL, INCREASE FUEL FLOW");
		} else if (plasmaSize > 1.4f){
			sizeCheck.setMessage("PLASMA TOO LARGE, DECREASE FUEL FLOW");
		} else {
			sizeCheck.isOk = true;
		}
		returnProblems.add(sizeCheck);
		
		//now for heat checks
		ReactorResource heatRes = resourceStore.get("HEAT");
		ReactorCheck heatCheck = new ReactorCheck("REACTORTEMP", false);
		if(heatRes.getAmount() > heatRes.maxAmount * 0.66f){
			heatCheck.setMessage("REACTOR TEMPERATURE CRITICAL");
		} else {
			heatCheck.isOk = true;
		}
		returnProblems.add(heatCheck);
		
		

		return returnProblems;
	}

}
