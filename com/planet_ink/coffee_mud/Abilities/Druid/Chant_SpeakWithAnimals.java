package com.planet_ink.coffee_mud.Abilities.Druid;
import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.core.*;
import com.planet_ink.coffee_mud.core.collections.*;
import com.planet_ink.coffee_mud.Abilities.interfaces.*;
import com.planet_ink.coffee_mud.Areas.interfaces.*;
import com.planet_ink.coffee_mud.Behaviors.interfaces.*;
import com.planet_ink.coffee_mud.CharClasses.interfaces.*;
import com.planet_ink.coffee_mud.Commands.interfaces.*;
import com.planet_ink.coffee_mud.Common.interfaces.*;
import com.planet_ink.coffee_mud.Exits.interfaces.*;
import com.planet_ink.coffee_mud.Items.interfaces.*;
import com.planet_ink.coffee_mud.Libraries.interfaces.*;
import com.planet_ink.coffee_mud.Locales.interfaces.*;
import com.planet_ink.coffee_mud.MOBS.interfaces.*;
import com.planet_ink.coffee_mud.Races.interfaces.*;

import java.util.*;

/*
   Copyright 2003-2015 Bo Zimmerman

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

@SuppressWarnings("rawtypes")
public class Chant_SpeakWithAnimals extends Chant
{
	@Override public String ID() { return "Chant_SpeakWithAnimals"; }
	private final static String localizedName = CMLib.lang().L("Speak With Animals");
	@Override public String name() { return localizedName; }
	private final static String localizedStaticDisplay = CMLib.lang().L("(Speak With Animals)");
	@Override public String displayText() { return localizedStaticDisplay; }
	@Override public int abstractQuality(){return Ability.QUALITY_BENEFICIAL_SELF;}
	@Override public int classificationCode(){return Ability.ACODE_CHANT|Ability.DOMAIN_ANIMALAFFINITY;}

	protected WeakHashMap<MOB,Behavior> mudChatters=new WeakHashMap<MOB,Behavior>();
	protected Map<String,Language> myLanguages=new Hashtable<String,Language>();
	
	@Override
	public void unInvoke()
	{
		// undo the affects of this spell
		if(!(affected instanceof MOB))
			return;
		final MOB mob=(MOB)affected;
		super.unInvoke();
		if(canBeUninvoked())
		{
			if((mob.location()!=null)&&(!mob.amDead()))
				mob.location().show(mob,null,CMMsg.MSG_OK_VISUAL,L("<S-YOUPOSS> ability to speak to animals has faded."));
			mudChatters.clear();
		}
	}

	protected Language getMyAnimalSpeak(final MOB M, final String ID)
	{
		if((M!=null)
		&&(ID!=null)
		&&(ID.length()>0))
		{
			synchronized(myLanguages)
			{
				Language lA=myLanguages.get(ID);
				if(lA!=null)
					return lA;
				lA=(Language)CMClass.getAbility(ID);
				lA.setProficiency(100);
				lA.setAffectedOne(M);
				lA.setBeingSpoken(lA.ID(), true);
				myLanguages.put(ID, lA);
				return lA;
			}
		}
		return null;
	}

	
	protected Language getAnimalSpeak(final MOB M)
	{
		if((M!=null)
		&&(CMLib.flags().isAnimalIntelligence(M)))
		{
			final Race r=M.charStats().getMyRace();
			for(Ability A : r.racialAbilities(M))
			{
				if(((A.classificationCode()&Ability.ALL_ACODES)==Ability.ACODE_LANGUAGE)
				&&(A instanceof Language))
				{
					A=M.fetchEffect(A.ID());
					if((A!=null)&&(((Language)A).beingSpoken(A.ID())))
						return (Language)A;
				}
			}
		}
		return null;
	}

	protected Behavior getMudChat(final MOB M)
	{
		if((M!=null)
		&&(CMLib.flags().isAnimalIntelligence(M)))
		{
			Behavior B = null;
			synchronized(mudChatters)
			{
				if(mudChatters.containsKey(M))
				{
					return mudChatters.get(M);
				}
				if(getAnimalSpeak(M)!=null)
				{
					B=CMClass.getBehavior("MudChat");
					B.setParms("="); // ensures the most simple
				}
				mudChatters.put(M, B);
			}
			return B;
		}
		return null;
	}
	
	@Override 
	public boolean tick(Tickable ticking, int tickID)
	{
		if(!super.tick(ticking, tickID))
			return false;
		final Physical affected=this.affected;
		if(affected instanceof MOB)
		{
			final MOB mob=(MOB)affected;
			final Room room=mob.location();
			if(room != null)
			{
				for(Enumeration<MOB> m = room.inhabitants(); m.hasMoreElements(); )
				{
					final MOB M=m.nextElement();
					if((M!=null)
					&&(M!=mob)
					&&(CMLib.flags().isAnimalIntelligence(M)))
					{
						final Behavior B=getMudChat(M);
						if(B!=null)
							B.tick(M, Tickable.TICKID_MOB);
					}
				}
			}
		}
		return true;
	}

	@Override
	public void executeMsg(final Environmental myHost, final CMMsg msg)
	{
		super.executeMsg(myHost,msg);
		final Physical affected=this.affected;
		if(affected instanceof MOB)
		{
			final MOB mob=(MOB)affected;
			if(((msg.sourceMinor()==CMMsg.TYP_SPEAK)
			   ||(msg.sourceMinor()==CMMsg.TYP_TELL)
			   ||(CMath.bset(msg.sourceMajor(),CMMsg.MASK_CHANNEL)))
			&&(msg.sourceMessage()!=null))
			{
				if((!msg.amISource(mob))
				&&(msg.tool() instanceof Ability)
				&&((((Ability)msg.tool()).classificationCode()&Ability.ALL_ACODES)==Ability.ACODE_LANGUAGE)
				&&(CMLib.flags().isAnimalIntelligence(msg.source()))
				&&(mob.fetchEffect(msg.tool().ID())==null)
				&&(msg.source().charStats().getMyRace().racialAbilities(msg.source()).find(msg.tool().ID())!=null))
				{
					final String str=CMStrings.getSayFromMessage(msg.sourceMessage());
					if(str!=null)
					{
						if(CMath.bset(msg.sourceMajor(),CMMsg.MASK_CHANNEL))
							msg.addTrailerMsg(CMClass.getMsg(msg.source(),null,null,CMMsg.NO_EFFECT,CMMsg.NO_EFFECT,msg.othersCode(),L("@x1 (translated from @x2)",CMStrings.substituteSayInMessage(msg.othersMessage(),str),msg.tool().name())));
						else
						if(msg.amITarget(affected)&&(msg.targetMessage()!=null))
							msg.addTrailerMsg(CMClass.getMsg(msg.source(),affected,null,CMMsg.NO_EFFECT,msg.targetCode(),CMMsg.NO_EFFECT,L("@x1 (translated from @x2)",CMStrings.substituteSayInMessage(msg.targetMessage(),str),msg.tool().name())));
						else
						if((msg.othersMessage()!=null)&&(msg.othersMessage().indexOf('\'')>0))
						{
							String otherMes=msg.othersMessage();
							if(msg.target()!=null)
								otherMes=CMLib.coffeeFilter().fullOutFilter((mob).session(),mob,msg.source(),msg.target(),msg.tool(),otherMes,false);
							msg.addTrailerMsg(CMClass.getMsg(msg.source(),affected,null,CMMsg.NO_EFFECT,msg.othersCode(),CMMsg.NO_EFFECT,L("@x1 (translated from @x2)",CMStrings.substituteSayInMessage(otherMes,str),msg.tool().name())));
						}
					}
				}
				else
				if(msg.amISource(mob)
				&&(msg.target() instanceof MOB)
				&&(CMLib.flags().isAnimalIntelligence((MOB)msg.target()))
				&&((msg.tool()==null) || (!(msg.tool() instanceof Language)) ||(((MOB)msg.target()).charStats().getMyRace().racialAbilities((MOB)msg.target()).find(msg.tool().ID())==null)))
				{
					Language lA=this.getAnimalSpeak((MOB)msg.target());
					if(lA!=null)
						lA=getMyAnimalSpeak(mob,lA.ID());
					if(lA!=null)
						lA.executeMsg(mob, msg);
				}
			}
			final Room room=mob.location();
			if(room != null)
			{
				for(Enumeration<MOB> m = room.inhabitants(); m.hasMoreElements(); )
				{
					final MOB M=m.nextElement();
					if((M!=null)
					&&(M!=mob)
					&&(CMLib.flags().isAnimalIntelligence(M)))
					{
						final Behavior B=getMudChat(M);
						if(B!=null)
							B.executeMsg(M, msg);
					}
				}
			}
		}
	}

	@Override
	public boolean okMessage(final Environmental myHost, final CMMsg msg)
	{
		if(!super.okMessage(myHost, msg))
			return false;
		final Physical affected=this.affected;
		if(affected instanceof MOB)
		{
			final MOB mob=(MOB)affected;
			final Room room=mob.location();
			if(room != null)
			{
				if(((msg.sourceMinor()==CMMsg.TYP_SPEAK)
				   ||(msg.sourceMinor()==CMMsg.TYP_TELL)
				   ||(CMath.bset(msg.sourceMajor(),CMMsg.MASK_CHANNEL)))
				&&(msg.sourceMessage()!=null))
				{
					if(msg.amISource(mob)
					&&(msg.target() instanceof MOB)
					&&(CMLib.flags().isAnimalIntelligence((MOB)msg.target()))
					&&((msg.tool()==null) || (!(msg.tool() instanceof Language)) ||(((MOB)msg.target()).charStats().getMyRace().racialAbilities((MOB)msg.target()).find(msg.tool().ID())==null)))
					{
						Language lA=this.getAnimalSpeak((MOB)msg.target());
						if(lA!=null)
							lA=getMyAnimalSpeak(mob,lA.ID());
						if(lA!=null)
							lA.okMessage(mob, msg);
					}
				}
				for(Enumeration<MOB> m = room.inhabitants(); m.hasMoreElements(); )
				{
					final MOB M=m.nextElement();
					if((M!=null)
					&&(M!=mob)
					&&(CMLib.flags().isAnimalIntelligence(M)))
					{
						final Behavior B=getMudChat(M);
						if((B!=null)&&(!B.okMessage(M, msg)))
							return false;
					}
				}
			}
		}
		return true;
	}

	@Override
	public boolean invoke(MOB mob, Vector commands, Physical givenTarget, boolean auto, int asLevel)
	{
		MOB target=mob;
		if((auto)&&(givenTarget!=null)&&(givenTarget instanceof MOB))
			target=(MOB)givenTarget;

		if(target.fetchEffect(this.ID())!=null)
		{
			mob.tell(target,null,null,L("<S-YOUPOSS> can already speak with animals."));
			return false;
		}

		if(!super.invoke(mob,commands,givenTarget,auto,asLevel))
			return false;

		boolean success=proficiencyCheck(mob,0,auto);

		if(success)
		{
			invoker=mob;
			final CMMsg msg=CMClass.getMsg(mob,null,this,verbalCastCode(mob,null,auto),auto?L("<T-NAME> attain(s) the ability to speak with aniamls!"):L("^S<S-NAME> chant(s) to <S-NAMESELF>, becoming one with the speech of animals!^?"));
			if(mob.location().okMessage(mob,msg))
			{
				mob.location().send(mob,msg);
				success=beneficialAffect(mob,target,asLevel,0)!=null;
				target.location().recoverRoomStats();
			}
		}
		else
			return beneficialWordsFizzle(mob,null,L("<S-NAME> chant(s) to <S-NAMESELF>, but nothing happens"));

		// return whether it worked
		return success;
	}
}
