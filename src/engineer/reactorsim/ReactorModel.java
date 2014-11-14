package engineer.reactorsim;

import java.util.ArrayList;



import common.ConsoleLogger;

import processing.core.PVector;

public class ReactorModel {

	
	float powerLevel = 100f;
	
	ArrayList<ReactorSystem> systems = new ArrayList<ReactorSystem>();
	
	
	
	public ArrayList<ReactorSystem> getSystems() {
		return systems;
	}

	public ReactorModel() {
		CoolantValveSystem coolantValves = new CoolantValveSystem();
		coolantValves.setScreenPosition(new PVector(300,300));
		systems.add(coolantValves);
		
		CoolantMixerSystem coolantMixer = new CoolantMixerSystem();
		coolantMixer.setScreenPosition(new PVector(300, 380));
		systems.add(coolantMixer);
		
		coolantValves.addInboundConnection(coolantMixer);
		
		//reactor vessel
		ReactorVesselSystem rvSystem = new ReactorVesselSystem();
		rvSystem.setScreenPosition(new PVector(460, 280));
		rvSystem.addInboundConnection(coolantValves);
		systems.add(rvSystem);
		
		TurbineSystem turbines = new TurbineSystem();
		turbines.setScreenPosition(new PVector(460, 600));
		turbines.addInboundConnection(rvSystem);
		systems.add(turbines);
		
		PowerDistributionSystem powerSys = new PowerDistributionSystem();
		powerSys.setScreenPosition(new PVector(650, 600));
		powerSys.addInboundConnection(turbines);
		systems.add(powerSys);
	}
	
	//simulate one iteration of the reactor model
	public void tick(){
		
		for(ReactorSystem r : systems){
			r.tick();
		}
	
	}
	
	public void damageReactor(float amount){
		ConsoleLogger.log(this, "Damage to reactor: " + amount);
		int numberToDamage = (int)(Math.random() * amount * 0.2f);
		while (numberToDamage > 0){
			//pick a random system and apply damage
			int ind = (int) Math.floor(Math.random() * systems.size());
			ReactorSystem rand = systems.get(ind);
			ConsoleLogger.log(this, "damaged: " + rand.getClass().getName());
			rand.applyDamage(amount);
			numberToDamage--;
			
			
		}
	}
	
	
	
	
	
	
	

}
