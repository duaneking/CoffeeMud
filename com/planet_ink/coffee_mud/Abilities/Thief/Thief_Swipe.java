package com.planet_ink.coffee_mud.Abilities.Thief;

import com.planet_ink.coffee_mud.interfaces.*;
import com.planet_ink.coffee_mud.common.*;
import com.planet_ink.coffee_mud.utils.*;
import java.util.*;

/* 
   Copyright 2000-2005 Bo Zimmerman

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
public class Thief_Swipe extends ThiefSkill
{
	public String ID() { return "Thief_Swipe"; }
	public String name(){ return "Swipe gold";}
	protected int canAffectCode(){return 0;}
	protected int canTargetCode(){return CAN_MOBS;}
	public int quality(){return Ability.MALICIOUS;}
	private static final String[] triggerStrings = {"SWIPE"};
	public String[] triggerStrings(){return triggerStrings;}
	public int usageType(){return USAGE_MOVEMENT|USAGE_MANA;}
	public int code=0;

	public int abilityCode(){return code;}
	public void setAbilityCode(int newCode){code=newCode;}

	private DVector lastOnes=new DVector(2);
	private int timesPicked(MOB target)
	{
		int times=0;
		for(int x=0;x<lastOnes.size();x++)
		{
			MOB M=(MOB)lastOnes.elementAt(x,1);
			Integer I=(Integer)lastOnes.elementAt(x,2);
			if(M==target)
			{
				times=I.intValue();
				lastOnes.removeElement(M);
				break;
			}
		}
		if(lastOnes.size()>=50)
			lastOnes.removeElementAt(0);
		lastOnes.addElement(target,new Integer(times+1));
		return times+1;
	}


	public boolean invoke(MOB mob, Vector commands, Environmental givenTarget, boolean auto, int asLevel)
	{
		if((commands.size()<1)&&(givenTarget==null))
		{
			mob.tell("Swipe from whom?");
			return false;
		}
		if(mob.isInCombat())
		{
			mob.tell("Not while you are fighting!");
			return false;
		}

		MOB target=this.getTarget(mob,commands,givenTarget);
		if(target==null) return false;

		int levelDiff=target.envStats().level()-(mob.envStats().level()+abilityCode());

		if((!target.mayIFight(mob))&&(levelDiff<15))
		{
			mob.tell("You cannot swipe from "+target.charStats().himher()+".");
			return false;
		}
		if(!super.invoke(mob,commands,givenTarget,auto,asLevel))
			return false;

		String currency=BeanCounter.getCurrency(target);
		int discoverChance=(target.charStats().getStat(CharStats.WISDOM)*5)-(levelDiff*3);
		int times=timesPicked(target);
		if(times>5) discoverChance-=(20*(times-5));
		if(!Sense.canBeSeenBy(mob,target))
			discoverChance+=50;
		if(discoverChance>95) discoverChance=95;
		if(discoverChance<5) discoverChance=5;

		if(levelDiff>0)
			levelDiff=-(levelDiff*((!Sense.canBeSeenBy(mob,target))?5:15));
		else
			levelDiff=-(levelDiff*((!Sense.canBeSeenBy(mob,target))?1:2));
		boolean success=profficiencyCheck(mob,levelDiff,auto);

		if(!success)
		{
			if(Dice.rollPercentage()>discoverChance)
			{
				FullMsg msg=new FullMsg(mob,target,this,CMMsg.MSG_NOISYMOVEMENT,auto?"":"You fumble the swipe; <T-NAME> spots you!",CMMsg.MSG_NOISYMOVEMENT,auto?"":"<S-NAME> tries to pick your pocket and fails!",CMMsg.MSG_OK_VISUAL,auto?"":"<S-NAME> tries to pick <T-NAME>'s pocket and fails!");
				if(mob.location().okMessage(mob,msg))
					mob.location().send(mob,msg);
			}
			else
				mob.tell(auto?"":"You fumble the swipe.");
		}
		else
		{
			double pct=0.25;
			if(levelDiff>0) pct=0.15;
			if(levelDiff>5) pct=0.10;
			if(levelDiff>10) pct=0.05;
			double goldTaken=BeanCounter.getTotalAbsoluteNativeValue(target)*pct*Math.random();
			if(goldTaken<new Integer(CMAble.qualifyingClassLevel(mob,this)).doubleValue())
				goldTaken=new Integer(CMAble.qualifyingClassLevel(mob,this)).doubleValue();
			if(goldTaken>BeanCounter.getTotalAbsoluteNativeValue(target)) goldTaken=BeanCounter.getTotalAbsoluteNativeValue(target);
			String goldTakenStr=BeanCounter.nameCurrencyShort(target,goldTaken);
			
			String str=null;
			int code=CMMsg.MSG_THIEF_ACT;
			if(!auto)
				if(goldTaken > 0)
					str="<S-NAME> pick(s) <T-HIS-HER> pocket for "+goldTakenStr+".";
				else
				{
					str="<S-NAME> attempt(s) to pick <T-HIS-HER> pocket, but nothing was found to steal!";
					code=CMMsg.MSG_QUIETMOVEMENT;
				}

			boolean alreadyFighting=(mob.getVictim()==target)||(target.getVictim()==mob);
			String hisStr=str;
			int hisCode=CMMsg.MSG_THIEF_ACT;
			if(Dice.rollPercentage()<discoverChance)
				hisStr=null;
			else
			{
				str+=" <T-NAME> spots you!";
				hisCode=hisCode|((target.mayIFight(mob))?CMMsg.MASK_MALICIOUS:0);
			}

			FullMsg msg=new FullMsg(mob,target,this,code,str,hisCode,hisStr,CMMsg.NO_EFFECT,null);
			if(mob.location().okMessage(mob,msg))
			{
				mob.location().send(mob,msg);
				
				if((!target.isMonster())&&(mob.isMonster())&&(!alreadyFighting))
				{
					if(target.getVictim()==mob)
						target.makePeace();
					if(mob.getVictim()==target)
						mob.makePeace();
				}
				else
				if(((hisStr==null)||mob.isMonster())
				&&(!alreadyFighting)
				&&(Dice.rollPercentage()>goldTaken))
				{
					if(target.getVictim()==mob)
						target.makePeace();
					if(mob.getVictim()==target)
						mob.makePeace();
				}
				BeanCounter.addMoney(mob,currency,goldTaken);
				mob.recoverEnvStats();
				BeanCounter.subtractMoney(target,currency,goldTaken);
				target.recoverEnvStats();
			}
		}
		return success;
	}

}
