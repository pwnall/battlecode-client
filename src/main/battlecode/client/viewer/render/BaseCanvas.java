package battlecode.client.viewer.render;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Observer;

import javax.swing.*;

public abstract class BaseCanvas extends JPanel {

	protected Runnable spaceBarListener = null;

	public BaseCanvas() {
		super();

		setVisible(false);
		addHierarchyListener(new HierarchyListener() {
			public void hierarchyChanged(HierarchyEvent e) {
				if (isDisplayable()) {
					initKeyBindings();
					removeHierarchyListener(this);
				}
			}
		});
		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				if (getRenderer() != null) { getRenderer().setCanvasSize(getSize()); }
			}
		});
		setAlignmentX(CENTER_ALIGNMENT);
		setBackground(Color.BLACK);
		setMaximumSize(new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
		setPreferredSize(new Dimension(800, 600));
		setOpaque(true);

	}

	protected void initKeyBindings() {
		InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
		im.put(KeyStroke.getKeyStroke("ESCAPE"), "exit");
		for (char c = 'A'; c<='Z'; c++) {
			im.put(KeyStroke.getKeyStroke(String.valueOf(c)), "toggle");
		}
		if (RenderConfiguration.isTournamentMode()) {
			im.put(KeyStroke.getKeyStroke("SPACE"), "space");
			//im.put(KeyStroke.getKeyStroke("S"), "toggle");
		}
		getActionMap().put("toggle", new AbstractAction() {
			private static final long serialVersionUID = 0; // don't serialize
			public void actionPerformed(ActionEvent e) {
				if (getRenderer() != null) {
					getRenderer().handleAction(e.getActionCommand().charAt(0));
					repaint();
				}
			}
		});
		getActionMap().put("space", new AbstractAction() {
			private static final long serialVersionUID = 0; // don't serialize
			public void actionPerformed(ActionEvent e) {
				if (spaceBarListener != null) { spaceBarListener.run(); }
			}
		});
		getActionMap().put("exit", new AbstractAction() {
			private static final long serialVersionUID = 0; // don't serialize
			public void actionPerformed(ActionEvent e) { System.exit(0); }
		});
	}

	protected abstract BaseRenderer getRenderer();

	public void setSpaceBarListener(Runnable runnable) {
		spaceBarListener = runnable;
	}

	public abstract void setRenderer(BaseRenderer renderer);
	
	public abstract void setTournamentMode();

	public abstract void addPaintObserver(Observer o);
}