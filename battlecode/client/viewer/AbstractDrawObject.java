package battlecode.client.viewer;

import battlecode.common.ActionType;
import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotLevel;
import battlecode.common.RobotType;
import battlecode.common.Team;
import battlecode.client.viewer.render.RenderConfiguration;

import static battlecode.client.viewer.AbstractAnimation.AnimationType.*;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;

public abstract class AbstractDrawObject<Animation extends AbstractAnimation> {

	public static class RobotInfo {

		public final RobotType type;
		public final Team team;

		public RobotInfo(RobotType type, Team team) {
			this.type = type;
			this.team = team;
		}

		public boolean equals(Object obj) {
			return (obj instanceof RobotInfo &&
					((RobotInfo) obj).type == this.type &&
					((RobotInfo) obj).team == this.team);
		}

		public int hashCode() {
			return type.ordinal() + 561 * team.ordinal();
		}
	}

	public AbstractDrawObject(RobotType type, Team team) {
		info = new RobotInfo(type,team);
	}

	@SuppressWarnings("unchecked")
	public AbstractDrawObject(AbstractDrawObject<Animation> copy) {
		this(copy.info.type,copy.info.team);

		loc = copy.loc;
		dir = copy.dir;
		energon = copy.energon;
		flux = copy.flux;
		moving = copy.moving;
		targetLoc = copy.targetLoc;
		broadcast = copy.broadcast;
		controlBits = copy.controlBits;
		bytecodesUsed = copy.bytecodesUsed;
		System.arraycopy(copy.indicatorStrings, 0, indicatorStrings, 0,
						 GameConstants.NUMBER_OF_INDICATOR_STRINGS);

		for(Map.Entry<AbstractAnimation.AnimationType, Animation> entry: copy.animations.entrySet()) {
			animations.put(entry.getKey(),(Animation)entry.getValue().clone());
		}

		updateDrawLoc();
	}

	public abstract Animation createTeleportAnim(MapLocation src, MapLocation teleportLoc);
	public abstract Animation createDeathExplosionAnim(boolean isArchon);
	public abstract Animation createMortarAttackAnim(MapLocation target);
	public abstract Animation createEnergonTransferAnim(MapLocation loc, RobotLevel height, float amt, boolean isFlux);
	public abstract Animation createMortarExplosionAnim(Animation mortarAttackAnim);

	protected RobotInfo info;
	protected MapLocation loc;
	protected Direction dir;
	protected float drawX = 0, drawY = 0;
	protected int moving = 0;

	protected double energon = 0;
	protected double maxEnergon;
	protected double flux = 0;
	protected static final double production = GameConstants.ARCHON_PRODUCTION;
	protected int roundsUntilAttackIdle;
	protected int roundsUntilMovementIdle;

	protected ActionType attackAction = ActionType.IDLE;
	protected ActionType movementAction = ActionType.IDLE;

	protected MapLocation targetLoc = null;

	protected int broadcast = 0;

	protected long controlBits = 0;
	protected int bytecodesUsed = 0;

	protected Map<AbstractAnimation.AnimationType, Animation> animations = new EnumMap<AbstractAnimation.AnimationType, Animation>(AbstractAnimation.AnimationType.class) {
		private static final long serialVersionUID = 0;
		// if we've written an animation for one client but not the other, we don't
		// want to be putting null values in the animations list
		public Animation put(AbstractAnimation.AnimationType key, Animation value) {
			if(value!=null)
				return super.put(key,value);
			else
				return super.remove(key);
		}
	};
	
	protected String[] indicatorStrings =
		new String[GameConstants.NUMBER_OF_INDICATOR_STRINGS];

	public void setDirection(Direction d) {
		dir = d;
	}

	protected int moveDelay() {
		return dir.isDiagonal()?info.type.moveDelayDiagonal():info.type.moveDelayOrthogonal();
	}

	public long getBroadcast() {
		return broadcast;
	}

	public RobotType getType() {
		return info.type;
	}

	public Team getTeam() {
		return info.team;
	}

	public float getDrawX() {
		return loc.getX() + drawX;
	}

	public float getDrawY() {
		return loc.getY() + drawY;
	}

	public MapLocation getLocation() {
		return loc;
	}

	public MapLocation getTargetLoc() {
		return targetLoc;
	}

	public Direction getDirection() {
		return dir;
	}

	public double getEnergon() {
		return energon;
	}

	public double getProduction() {
		return production;
	}

	public double getFlux() {
		return flux;
	}

	public String getIndicatorString(int index) {
		return indicatorStrings[index];
	}

	public long getControlBits() {
		return controlBits;
	}

	public int getBytecodesUsed() {
		return bytecodesUsed;
	}

	public ActionType getAttackAction() {
		return attackAction;
	}

	public ActionType getMovementAction() {
		return movementAction;
	}

	public void setLocation(MapLocation loc) {
		this.loc = loc;
	}

	public void setBroadcast() {
		broadcast++;
	}

	public void setEnergon(double energon) {
		this.energon = energon;
	}

	public void setFlux(double flux){
		this.flux = flux;
	}

	public void setTeam(Team team) {
		info = new RobotInfo(info.type, team);
	}

	public void setString(int index, String newString) {
		indicatorStrings[index] = newString;
	}

	public void setControlBits(long controlBits) {
		this.controlBits = controlBits;
	}
	
	public void setBytecodesUsed(int used) {
		bytecodesUsed = used;
	}

	public boolean isAlive() {
		Animation deathAnim = animations.get(AbstractAnimation.AnimationType.DEATH_EXPLOSION);
		return deathAnim == null || deathAnim.isAlive() ||
			animations.get(AbstractAnimation.AnimationType.MORTAR_ATTACK)!=null ||
			animations.get(AbstractAnimation.AnimationType.MORTAR_EXPLOSION)!=null;
	}

	public void evolve(RobotType type) {
		movementAction = ActionType.TRANSFORMING;
		attackAction = ActionType.TRANSFORMING;
		//roundsUntilIdle = type.wakeDelay();
		info = new RobotInfo(type, info.team);
		maxEnergon = type.maxEnergon();
	}

	public void setAttacking(MapLocation target, RobotLevel height) {
		attackAction = ActionType.ATTACKING;
		roundsUntilAttackIdle = info.type.attackDelay();
		targetLoc = target;
		if (info.type == RobotType.CHAINER) {
			animations.put(MORTAR_ATTACK,createMortarAttackAnim(target));
		}
	}
	
	public void setMoving(boolean isMovingForward) {
		movementAction = ActionType.MOVING;
		moving = (isMovingForward ? 1 : -1);
		roundsUntilMovementIdle = (dir.isDiagonal()
								   ? info.type.moveDelayDiagonal()
								   : info.type.moveDelayOrthogonal());
		updateDrawLoc();
	}

	public void setMoving(boolean isMovingForward, int delay) {
		movementAction = ActionType.MOVING;
		moving = (isMovingForward ? 1 : -1);
		roundsUntilMovementIdle = delay;
		updateDrawLoc();
	}

	public void setTransfer(MapLocation loc, RobotLevel height, float amt, boolean isFlux) {
		animations.put(ENERGON_TRANSFER,createEnergonTransferAnim(loc, height,amt,isFlux));
	}

	public void setTeleport(MapLocation src, MapLocation loc) {
		animations.put(TELEPORT,createTeleportAnim(src,loc));
	}
	
	// remove this eventually
	public void drain() {
		attackAction = ActionType.DRAINING;
		roundsUntilAttackIdle = 1;
		//roundsUntilIdle = 2;
	}

	public void destroyUnit() {
		movementAction = ActionType.IDLE;
		attackAction = ActionType.IDLE;
		energon = 0;
		flux = 0;
		animations.put(DEATH_EXPLOSION,createDeathExplosionAnim(getType() == RobotType.ARCHON));
		animations.remove(ENERGON_TRANSFER);
	}

	public void updateRound() {

		if (roundsUntilMovementIdle == 0) {
			movementAction = ActionType.IDLE;
			moving = 0;
		}
		
		if (roundsUntilAttackIdle == 0) {
			attackAction = ActionType.IDLE;
		}

		updateDrawLoc();

		broadcast = (broadcast << 1) & 0x000FFFFF;
		//objectTenure++;
		//if (burnAmount > 0) burnAmount--;
		if(roundsUntilMovementIdle>0) roundsUntilMovementIdle--;
		if(roundsUntilAttackIdle>0) roundsUntilAttackIdle--;

		Iterator<Map.Entry<AbstractAnimation.AnimationType, Animation>> it = animations.entrySet().iterator();
		Map.Entry<AbstractAnimation.AnimationType, Animation> entry;
		Animation mortarExplosionAnim = null;
		while(it.hasNext()) {
			entry = it.next();
			entry.getValue().updateRound();
			if(!entry.getValue().isAlive()) {
				if(entry.getKey()==MORTAR_ATTACK) {
					mortarExplosionAnim = createMortarExplosionAnim(entry.getValue());
				}
				if(entry.getKey()!=DEATH_EXPLOSION)
					it.remove();
			}
		}
		if(mortarExplosionAnim!=null)
			animations.put(MORTAR_EXPLOSION,mortarExplosionAnim);

	}

	public void updateDrawLoc() {
		if (RenderConfiguration.showDiscrete() ||
			movementAction != ActionType.MOVING) {
			drawX = drawY = 0;
		} else {
			// HACK: implement a secondary counter for movment cooldown
			/*
			  if (action == ActionType.TRANSFORMING || getType().isBuilding())
			  return;
			*/

			
			float dist = (float) moving * roundsUntilMovementIdle / moveDelay();
			drawX = -dist * dir.dx;
			drawY = -dist * dir.dy;
			/*
			  if (info.type.isAirborne()) {
			  drawY -= 0.4f;
			  }
			*/
		}
	}

}