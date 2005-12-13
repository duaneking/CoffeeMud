package com.planet_ink.coffee_mud.Abilities.Fighter;
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

public class Fighter_ShieldBlock extends FighterSkill
{
	public int hits=0;
	public String ID() { return "Fighter_ShieldBlock"; }
	public String name(){ return "Shield Block";}
	public String displayText(){return "";};
	public int quality(){return Ability.BENEFICIAL_SELF;}
	protected int canAffectCode(){return Ability.CAN_MOBS;}
	protected int canTargetCode(){return 0;}
	public boolean isAutoInvoked(){return true;}
	public boolean canBeUninvoked(){return false;}
	public int classificationCode(){ return Ability.SKILL; }

	public boolean okMessage(Environmental myHost, CMMsg msg)
	{
		if((affected==null)||(!(affected instanceof MOB)))
			return true;

		MOB mob=(MOB)affected;

		if(msg.amITarget(mob)
		&&(msg.targetMinor()==CMMsg.TYP_WEAPONATTACK)
		&&(CMLib.flags().aliveAwakeMobileUnbound(mob,true))
		&&(msg.tool()!=null)
		&&(msg.tool() instanceof Weapon)
		&&(profficiencyCheck(null,mob.charStats().getStat(CharStats.DEXTERITY)-90,false))
		&&(mob.fetchFirstWornItem(Item.HELD) instanceof Shield)
		&&(msg.source().getVictim()==mob))
		{
			CMMsg msg2=CMClass.getMsg(msg.source(),mob,mob.fetchFirstWornItem(Item.HELD),CMMsg.MSG_QUIETMOVEMENT,"<T-NAME> block(s) <S-YOUPOSS> attack with <O-NAME>!");
			if(mob.location().okMessage(mob,msg2))
			{
				mob.location().send(mob,msg2);
				helpProfficiency(mob);
				return false;
			}
		}
		return true;
	}
}