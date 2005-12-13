package com.planet_ink.coffee_mud.Commands;
import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.core.*;
import com.planet_ink.coffee_mud.Abilities.interfaces.*;
import com.planet_ink.coffee_mud.Areas.interfaces.*;
import com.planet_ink.coffee_mud.Behaviors.interfaces.*;
import com.planet_ink.coffee_mud.CharClasses.interfaces.*;
import com.planet_ink.coffee_mud.Commands.interfaces.*;
import com.planet_ink.coffee_mud.Common.interfaces.*;
import com.planet_ink.coffee_mud.Exits.interfaces.*;
import com.planet_ink.coffee_mud.Items.interfaces.*;
import com.planet_ink.coffee_mud.Locales.interfaces.*;
import com.planet_ink.coffee_mud.MOBS.interfaces.*;
import com.planet_ink.coffee_mud.Races.interfaces.*;


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
public class Train extends StdCommand
{
	public Train(){}

	private String[] access={"TRAIN","TR","TRA"};
	public String[] getAccessWords(){return access;}
	public boolean execute(MOB mob, Vector commands)
		throws java.io.IOException
	{
		if(commands.size()<2)
		{
			mob.tell("You have "+mob.getTrains()+" training CMLib.sessions(). Enter HELP TRAIN for more information.");
			return false;
		}
		commands.removeElementAt(0);

		String abilityName=((String)commands.elementAt(0)).toUpperCase();
        StringBuffer thingsToTrainFor=new StringBuffer("");
        for(int i=0;i<CharStats.TRAITS.length;i++)
            if(i<CharStats.NUM_BASE_STATS)
                thingsToTrainFor.append(CharStats.TRAITS[i]+", ");
        
		int trainsRequired=1;
		int abilityCode=mob.baseCharStats().getCode(abilityName);
		int curStat=-1;
		if((abilityCode>=0)&&(abilityCode<CharStats.NUM_BASE_STATS))
		{
			CharStats copyStats=(CharStats)mob.baseCharStats().copyOf();
			mob.charStats().getMyRace().affectCharStats(mob,copyStats);
			for(int c=0;c<mob.charStats().numClasses();c++)
				mob.charStats().getMyClass(c).affectCharStats(mob,copyStats);
			curStat=copyStats.getStat(abilityCode);
			if(curStat<18)
				trainsRequired=1;
			else
			if(curStat<22)
				trainsRequired=2;
			else
			if(curStat<25)
				trainsRequired=3;
			
			if(curStat>=(CMProps.getIntVar(CMProps.SYSTEMI_BASEMAXSTAT)
						 +mob.charStats().getStat(CharStats.MAX_STRENGTH_ADJ+abilityCode)))
			{
				mob.tell("You cannot train that any further.");
				return false;
			}
		}
        else
            abilityCode=-1;
		CharClass theClass=null;
		if((!CMProps.getVar(CMProps.SYSTEM_MULTICLASS).startsWith("NO"))
		&&(abilityCode<0))
		{
			for(Enumeration c=CMClass.charClasses();c.hasMoreElements();)
			{
				CharClass C=(CharClass)c.nextElement();
                int classLevel=mob.charStats().getClassLevel(C);
                if(classLevel<0) classLevel=0;
				if((C.name().toUpperCase().startsWith(abilityName.toUpperCase()))
		        ||(C.name(classLevel).toUpperCase().startsWith(abilityName.toUpperCase())))
				{
                    if((C.qualifiesForThisClass(mob,false))
                    &&(!Util.bset(C.availabilityCode(),Area.THEME_SKILLONLYMASK)))
					{
						abilityCode=106;
						theClass=C;
					}
					break;
				}
                else
                if((C.qualifiesForThisClass(mob,true))
                &&(!Util.bset(C.availabilityCode(),Area.THEME_SKILLONLYMASK)))
                    thingsToTrainFor.append(C.name()+", ");
			}
		}

		if(abilityCode<0)
		{
			if("HIT POINTS".startsWith(abilityName.toUpperCase()))
				abilityCode=101;
			else
			if("MANA".startsWith(abilityName.toUpperCase()))
				abilityCode=102;
			else
			if("MOVE".startsWith(abilityName.toUpperCase()))
				abilityCode=103;
			else
			if("GAIN".startsWith(abilityName.toUpperCase()))
				abilityCode=104;
			else
			if("PRACTICES".startsWith(abilityName.toUpperCase()))
				abilityCode=105;
			else
			{
				mob.tell("You can't train for '"+abilityName+"'. Try "+thingsToTrainFor.toString()+"HIT POINTS, MANA, MOVE, GAIN, or PRACTICES.");
				return false;
			}
		}
		commands.removeElementAt(0);

		if(abilityCode==104)
		{
			if(mob.getPractices()<7)
			{
				mob.tell("You don't seem to have enough practices to do that.");
				return false;
			}
		}
		else
		if(mob.getTrains()==0)
		{
			mob.tell("You don't seem to have enough training sessions to do that.");
			return false;
		}
		else
		if(mob.getTrains()<trainsRequired)
		{
			mob.tell("Training that ability further will require "+trainsRequired+" training points.");
			return false;
		}

		MOB teacher=null;
		if(commands.size()>0)
		{
			teacher=mob.location().fetchInhabitant((String)commands.elementAt(0));
			if(teacher!=null) commands.removeElementAt(0);
		}
		if(teacher==null)
		for(int i=0;i<mob.location().numInhabitants();i++)
		{
			MOB possTeach=mob.location().fetchInhabitant(i);
			if((possTeach!=null)&&(possTeach!=mob))
			{
				teacher=possTeach;
				break;
			}
		}
		if((teacher==null)||((teacher!=null)&&(!CMLib.flags().canBeSeenBy(teacher,mob))))
		{
			mob.tell("That person doesn't seem to be here.");
			return false;
		}
		if(teacher==mob)
		{
			mob.tell("You cannot train with yourself!");
			return false;
		}
		if(Util.bset(teacher.getBitmap(),MOB.ATT_NOTEACH))
		{
			mob.tell(teacher.name()+" is refusing to teach right now.");
			return false;
		}
		if(Util.bset(mob.getBitmap(),MOB.ATT_NOTEACH))
		{
			mob.tell("You are refusing training at this time.");
			return false;
		}
		if(CMLib.flags().isSleeping(mob)||CMLib.flags().isSitting(mob))
		{
		    mob.tell("You need to stand up for your training.");
		    return false;
		}
		if(CMLib.flags().isSleeping(teacher)||CMLib.flags().isSitting(teacher))
		{
		    if(teacher.isMonster()) CMLib.commands().stand(teacher,true);
			if(CMLib.flags().isSleeping(teacher)||CMLib.flags().isSitting(teacher))
			{
			    mob.tell(teacher.name()+" looks a bit too relaxed to train with you.");
			    return false;
			}
		}
		if(mob.isInCombat())
		{
		    mob.tell("Not while you are fighting!");
		    return false;
		}
		if(teacher.isInCombat())
		{
		    mob.tell("Your teacher seems busy right now.");
		    return false;
		}

		if((abilityCode==106)
		&&(!teacher.charStats().getCurrentClass().baseClass().equals(mob.charStats().getCurrentClass().baseClass()))
		&&(!mob.charStats().getCurrentClass().baseClass().equals("Commoner"))
		&&(teacher.charStats().getClassLevel(theClass)<1))
	    {
			if((!CMProps.getVar(CMProps.SYSTEM_MULTICLASS).startsWith("MULTI")))
            {
                CharClass C=CMClass.getCharClass(mob.charStats().getCurrentClass().baseClass());
                String baseClassName=(C!=null)?C.name():mob.charStats().getCurrentClass().baseClass();
				mob.tell("You can only learn that from another "+baseClassName+".");
            }
			else
            {
                int classLevel=mob.charStats().getClassLevel(theClass);
                if(classLevel<0) classLevel=0;
				mob.tell("You can only learn that from another "+theClass.name(classLevel)+".");
            }
			return false;
		}

		if(abilityCode<100)
		{
			int teachStat=teacher.charStats().getStat(abilityCode);
			if(curStat>teachStat)
			{
				mob.tell("You can only train with someone whose score is higher than yours.");
				return false;
			}
			curStat=mob.baseCharStats().getStat(abilityCode);
		}

		CMMsg msg=CMClass.getMsg(teacher,mob,null,CMMsg.MSG_NOISYMOVEMENT,"<S-NAME> train(s) with <T-NAMESELF>.");
		if(!mob.location().okMessage(mob,msg))
			return false;
		mob.location().send(mob,msg);
		switch(abilityCode)
		{
		case 0:
			mob.tell("You feel stronger!");
			mob.baseCharStats().setStat(CharStats.STRENGTH,curStat+1);
			mob.recoverCharStats();
			mob.setTrains(mob.getTrains()-trainsRequired);
			break;
		case 1:
			mob.tell("You feel smarter!");
			mob.baseCharStats().setStat(CharStats.INTELLIGENCE,curStat+1);
			mob.recoverCharStats();
			mob.setTrains(mob.getTrains()-trainsRequired);
			break;
		case 2:
			mob.tell("You feel more dextrous!");
			mob.baseCharStats().setStat(CharStats.DEXTERITY,curStat+1);
			mob.recoverCharStats();
			mob.setTrains(mob.getTrains()-trainsRequired);
			break;
		case 3:
			mob.tell("You feel healthier!");
			mob.baseCharStats().setStat(CharStats.CONSTITUTION,curStat+1);
			mob.recoverCharStats();
			mob.setTrains(mob.getTrains()-trainsRequired);
			break;
		case 4:
			mob.tell("You feel more charismatic!");
			mob.baseCharStats().setStat(CharStats.CHARISMA,curStat+1);
			mob.recoverCharStats();
			mob.setTrains(mob.getTrains()-trainsRequired);
			break;
		case 5:
			mob.tell("You feel wiser!");
			mob.baseCharStats().setStat(CharStats.WISDOM,curStat+1);
			mob.recoverCharStats();
			mob.setTrains(mob.getTrains()-trainsRequired);
			break;
		case 101:
			mob.tell("You feel even healthier!");
			mob.baseState().setHitPoints(mob.baseState().getHitPoints()+10);
			mob.maxState().setHitPoints(mob.maxState().getHitPoints()+10);
			mob.curState().setHitPoints(mob.curState().getHitPoints()+10);
			mob.setTrains(mob.getTrains()-1);
			break;
		case 102:
			mob.tell("You feel more powerful!");
			mob.baseState().setMana(mob.baseState().getMana()+20);
			mob.maxState().setMana(mob.maxState().getMana()+20);
			mob.curState().setMana(mob.curState().getMana()+20);
			mob.setTrains(mob.getTrains()-1);
			break;
		case 103:
			mob.tell("You feel more rested!");
			mob.baseState().setMovement(mob.baseState().getMovement()+20);
			mob.maxState().setMovement(mob.maxState().getMovement()+20);
			mob.curState().setMovement(mob.curState().getMovement()+20);
			mob.setTrains(mob.getTrains()-1);
			break;
		case 104:
			mob.tell("You feel more trainable!");
			mob.setTrains(mob.getTrains()+1);
			mob.setPractices(mob.getPractices()-7);
			break;
		case 105:
			mob.tell("You feel more educatable!");
			mob.setTrains(mob.getTrains()-1);
			mob.setPractices(mob.getPractices()+5);
			break;
		case 106:
            int classLevel=mob.charStats().getClassLevel(theClass);
            if(classLevel<0) classLevel=0;
			mob.tell("You have undergone "+theClass.name(classLevel)+" training!");
			mob.setTrains(mob.getTrains()-1);
			mob.baseCharStats().getCurrentClass().endCharacter(mob);
			mob.baseCharStats().setCurrentClass(theClass);
			if((!mob.isMonster())&&(mob.soulMate()==null))
				CMLib.coffeeTables().bump(mob,CoffeeTableRow.STAT_CLASSCHANGE);
			mob.recoverCharStats();
			mob.charStats().getCurrentClass().startCharacter(mob,false,true);
			break;
		}
		return false;
	}
	public int ticksToExecute(){return 1;}
	public boolean canBeOrdered(){return false;}

	public int compareTo(Object o){ return CMClass.classID(this).compareToIgnoreCase(CMClass.classID(o));}
}
