package com.opendoorlogistics.studio.components.map.v2.plugins;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.SwingUtilities;

import com.opendoorlogistics.api.standardcomponents.map.MapApi;
import com.opendoorlogistics.api.standardcomponents.map.MapApiListeners.OnBuildContextMenu;
import com.opendoorlogistics.api.standardcomponents.map.MapApiListeners.OnBuildToolbarListener;
import com.opendoorlogistics.api.standardcomponents.map.MapToolbar;
import com.opendoorlogistics.api.tables.ODLTable;
import com.opendoorlogistics.api.tables.ODLTableReadOnly;
import com.opendoorlogistics.core.gis.map.data.DrawableObject;
import com.opendoorlogistics.core.gis.map.data.DrawableObjectImpl;
import com.opendoorlogistics.core.tables.beans.BeanMappedRow;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanTableMapping;
import com.opendoorlogistics.studio.components.map.v2.MapPopupMenuImpl;
import com.opendoorlogistics.studio.components.map.v2.MapToolbarImpl;
import com.opendoorlogistics.utils.ui.Icons;
import com.opendoorlogistics.api.standardcomponents.map.MapApiListeners.*;
import com.opendoorlogistics.api.standardcomponents.map.*;

public class PluginUtils {
	static interface ActionFactory{
		Action create(MapApi api);
	}

	static void registerActionFactory(final MapApi api,final ActionFactory factory, int priority,final String group, final long needsFlags){

		class WrapperAction implements Action{
			final Action decorated;
			final OnObjectsChanged listener;
			
			WrapperAction(OnObjectsChanged listener){
				decorated = factory.create(api);
				this.listener = listener;
			}
			
			public void actionPerformed(ActionEvent e) {
				decorated.actionPerformed(e);
			}

			public Object getValue(String key) {
				return decorated.getValue(key);
			}

			public void putValue(String key, Object value) {
				decorated.putValue(key, value);
			}

			public void setEnabled(boolean b) {
				decorated.setEnabled(b);
			}

			public boolean isEnabled() {
				return decorated.isEnabled();
			}

			public void addPropertyChangeListener(PropertyChangeListener listener) {
				decorated.addPropertyChangeListener(listener);
			}

			public void removePropertyChangeListener(PropertyChangeListener listener) {
				decorated.removePropertyChangeListener(listener);
			}
		
			public void updateEnabled(){
				boolean enabled = getIsEnabled(api, needsFlags);
				setEnabled(enabled);		
			}
		}
		
		final OnObjectsChanged listener = new OnObjectsChanged() {
			
			@Override
			public void onObjectsChanged(MapApi api) {
				MapToolbar toolbar = api.getMapToolbar();
				if(toolbar!=null){
					for(Action action : toolbar.getActions()){
						if(action!=null && WrapperAction.class.isInstance(action)){
							WrapperAction wa = (WrapperAction)action;
							if(wa.listener == this){
								wa.updateEnabled();
							}
						}
					}
				}
			}
		};
		

		api.registerObjectsChangedListener(listener, 0);
		
		api.registerOnBuildToolbarListener(new OnBuildToolbarListener() {
			
			@Override
			public void onBuildToolbar(MapApi api, MapToolbar toolBar) {
				toolBar.add(new WrapperAction(listener), group);
			}

		}, priority);
		
		
		api.registerOnBuildContextMenuListener(new OnBuildContextMenu() {
			
			@Override
			public void onBuildContextMenu(MapApi api, MapPopupMenu menu) {
				WrapperAction action = new WrapperAction(listener);
				action.updateEnabled();
				menu.add(action, group);
			}

		}, priority);
	}
	
	static void registerActionFactory(MapApi api,final ActionFactory factory, int priority,final String group){
		api.registerOnBuildToolbarListener(new OnBuildToolbarListener() {
			
			@Override
			public void onBuildToolbar(MapApi api, MapToolbar toolBar) {
				toolBar.add(factory.create(api), group);
			}

		}, priority);
		
		api.registerOnBuildContextMenuListener(new OnBuildContextMenu() {
			
			@Override
			public void onBuildContextMenu(MapApi api, MapPopupMenu menu) {
				menu.add(factory.create(api), group);
			}

		}, priority);
	}
	
	
	static void initAction(String name, String description, String iconName, Action action){
        action.putValue(Action.NAME, name);
        action.putValue(Action.SMALL_ICON, Icons.loadFromStandardPath(iconName));
        action.putValue(Action.SHORT_DESCRIPTION, description);
        action.putValue(Action.SHORT_DESCRIPTION, description);
	}
	
	static Cursor createCursor(String imagefile, int xhotspot, int yhotspot){
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		BufferedImage image;
		try {
			// Use own class loader to prevent problems when jar loaded by reflection
			URL url = PluginUtils.class.getResource("/resources/icons/" + imagefile);
			image = ImageIO.read(url);
		} catch (IOException e) {
			throw new RuntimeException();
		}
		// Image image = toolkit.getImage("/resources/icons/" + imagefile);
		Cursor cursor = toolkit.createCustomCursor(image, new Point(xhotspot, yhotspot), imagefile);
		return cursor;
	}
	
	static void drawRectangle(Graphics2D g, Rectangle rectangle, Color fillColour) {
		Color borderColour = new Color(0, 0, 0, 100);
		g.setColor(borderColour);
		g.draw(rectangle);
		g.setColor(fillColour);
		g.fill(rectangle);
	}
	
	static LinkedList<DrawableObject> getVisibleDrawables(final MapApi api) {
		ODLTableReadOnly drawablesTable = api.getMapDataApi().getFilteredAllLayersTable();
		return toDrawables(drawablesTable);
	}


	static LinkedList<DrawableObject> toDrawables(ODLTableReadOnly drawablesTable) {
		BeanTableMapping btm = DrawableObjectImpl.getBeanMapping().getTableMapping(0);
		LinkedList<DrawableObject> list = new LinkedList<DrawableObject>();
		for(BeanMappedRow r: btm.readObjectsFromTable(drawablesTable)){
			list.add((DrawableObject)r);
		}
		return list;
	}

	static boolean exitIfInMode(MapApi api, Class<?> modeClass){
		if(api.getMapMode()!=null && modeClass.isInstance(api.getMapMode())&& api.getDefaultMapMode()!=null ){
			api.setMapMode(api.getDefaultMapMode());
			return true;
		}
		return false;
		
	}

	static boolean getIsEnabled(final MapApi api, final long needsFlags) {
		ODLTable table = api.getMapDataApi().getUnfilteredActiveTable();
		boolean enabled = false;
		if(table!=null){
			enabled = (table.getFlags() & needsFlags) ==needsFlags;
		}
		return enabled;
	}
	
	static void exitModeIfNeeded(MapApi api, final Class<?> modeClass, long needsFlags,final boolean clearSelection) {
		if(!PluginUtils.getIsEnabled(api, needsFlags)){
			SwingUtilities.invokeLater(new Runnable() {
				
				@Override
				public void run() {
					PluginUtils.exitIfInMode(api, modeClass);
					if(clearSelection){
						api.clearSelection();
					}
				}
			});
		}
	}

}
