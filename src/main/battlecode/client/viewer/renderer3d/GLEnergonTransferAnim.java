package battlecode.client.viewer.renderer3d;

import javax.media.opengl.GL;


import javax.media.opengl.glu.GLU;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import battlecode.client.viewer.render.RenderConfiguration;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotLevel;

import battlecode.common.TerrainTile;

class GLEnergonTransferAnim extends GLAnimation {
	private final MapLocation target;
	private final RobotLevel targetHeight;
	private final float amount;
	private final float distance;
	
	private static final Vector4f energonColor = new Vector4f(0.0f, 1.0f, 0.0f, 0.5f);
	private static final Vector4f fluxColor = new Vector4f(1.0f, 0.0f, 1.0f, 0.5f);
	
	private Vector4f color;

	private GLDrawObject src;
	/*private static final int NUM_POINTS = 16;
	private static Vector3f[] points = null;*/

	public GLEnergonTransferAnim(GLDrawObject src, MapLocation target, RobotLevel height, float amount, boolean isFlux) {
		super(10);
		//super((int)Math.round(amount / GameConstants.ENERGON_TRANSFER_RATE));

		/*if(points == null) {
			points = new Vector3f[NUM_POINTS];
			for(int i = 0; i < NUM_POINTS; i++) {
				points[i] = new Vector3f((float)Math.random(), 0.0f, -0.05f + 0.1f * (float)Math.random());
			}
		}*/
		this.targetHeight = height;
		this.src = src;
		this.target = target;
		this.amount = amount;
		float Dx  = target.getX() - src.getDrawX();
		float Dy  = target.getY() - src.getDrawY();
		float len = (float)Math.hypot(Dx, Dy);
		this.distance = len;
		color = isFlux?fluxColor:energonColor;
	}

	public void setSource(GLDrawObject src) {
		this.src = src;
	}

	public void draw(GL gl, GLU glu) {
		// do nothing
	}

	public void draw(GL gl, GLDrawMap map, MapLocation origin) {
		if(!RenderConfiguration.showTransfers())
			return;
		
		
		Vector3f toTarget = new Vector3f();
		
		float airHeight = GLDrawMap.MAP_SCALE*32;//map.getTerrainHeight(target.getX() + 0.5f - origin.getX(), target.getY() + 0.5f - origin.getY());
		toTarget.x = target.getX() - src.getDrawX();
		toTarget.y = ((targetHeight == RobotLevel.IN_AIR) ? airHeight : map.getTerrainHeight(target.getX() + 0.5f - origin.getX(), target.getY() + 0.5f - origin.getY())) -
		((targetHeight == RobotLevel.IN_AIR) ? airHeight  : map.getTerrainHeight(src.getDrawX() - origin.getX(), src.getDrawY() - origin.getY()));
		toTarget.z = target.getY() - src.getDrawY();
		float time = Math.min(roundsToLive, lifetime-roundsToLive)/lifetime;

		gl.glLineWidth(8.0f * time);
		gl.glColor4f(color.x, color.y, color.z, color.w);
		gl.glBegin(GL.GL_LINES);
		gl.glVertex3f(0.0f, 0.1f, 0.0f);
		gl.glVertex3f(toTarget.x, toTarget.y, toTarget.z);
		gl.glEnd();
		gl.glLineWidth(1.0f);
	}

	public Object clone() {
		GLEnergonTransferAnim clone = new GLEnergonTransferAnim(src, target, targetHeight, amount, color==fluxColor);
		clone.roundsToLive = roundsToLive;
		return clone;
	}
}
