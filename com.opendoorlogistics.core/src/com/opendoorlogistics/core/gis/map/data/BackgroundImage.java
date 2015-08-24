package com.opendoorlogistics.core.gis.map.data;

import com.opendoorlogistics.api.components.PredefinedTags;
import com.opendoorlogistics.api.standardcomponents.map.MapTileProvider;
import com.opendoorlogistics.api.tables.TableFlags;
import com.opendoorlogistics.core.tables.beans.BeanMappedRowImpl;
import com.opendoorlogistics.core.tables.beans.BeanMapping;
import com.opendoorlogistics.core.tables.beans.BeanMapping.BeanTableMapping;
import com.opendoorlogistics.core.tables.beans.annotations.ODLColumnName;
import com.opendoorlogistics.core.tables.beans.annotations.ODLNullAllowed;
import com.opendoorlogistics.core.tables.beans.annotations.ODLTableFlags;
import com.opendoorlogistics.core.tables.beans.annotations.ODLTableName;

@ODLTableName(PredefinedTags.BACKGROUND_IMAGE)
@ODLTableFlags(TableFlags.FLAG_IS_OPTIONAL)
public class BackgroundImage extends BeanMappedRowImpl{
	public static final BeanTableMapping BEAN_MAPPING = BeanMapping.buildTable(BackgroundImage.class);
	
	private MapTileProvider tileProvider;

	public MapTileProvider getTileProvider() {
		return tileProvider;
	}

	@ODLColumnName("TileProvider")
	@ODLNullAllowed
	public void setTileProvider(MapTileProvider tileProvider) {
		this.tileProvider = tileProvider;
	}

}
