package com.eveningoutpost.dexdrip.tiles;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.wear.tiles.ColorBuilders;
import androidx.wear.tiles.DimensionBuilders;
import androidx.wear.tiles.LayoutElementBuilders;
import androidx.wear.tiles.ModifiersBuilders;
import androidx.wear.tiles.RequestBuilders;
import androidx.wear.tiles.ResourceBuilders;
import androidx.wear.tiles.TileBuilders;
import androidx.wear.tiles.TileService;
import androidx.wear.tiles.TimelineBuilders;

import com.eveningoutpost.dexdrip.modern.ModernBestGlucose;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.TimeUnit;

/**
 * Glucose Tile Service for Wear OS 5
 */
@RequiresApi(api = Build.VERSION_CODES.S)
public class GlucoseTileService extends TileService {
    
    private static final String TAG = "GlucoseTileService";
    private static final String RESOURCES_VERSION = "1";
    private static final long TILE_REFRESH_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(1);
    
    @NonNull
    @Override
    protected ListenableFuture<TileBuilders.Tile> onTileRequest(@NonNull RequestBuilders.TileRequest request) {
        // Get glucose data
        UserError.Log.d(TAG, "onTileRequest: Getting glucose data");
        ModernBestGlucose.DisplayGlucose dg = null;
        try {
            dg = ModernBestGlucose.getDisplayGlucose();
            UserError.Log.d(TAG, "onTileRequest: Got glucose data: " + (dg != null ? "not null" : "null"));
            if (dg != null) {
                UserError.Log.d(TAG, "onTileRequest: Glucose value: " + dg.unitized + ", arrow: " + dg.delta_arrow);
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "Error getting glucose data: " + e);
            e.printStackTrace();
        }
        
        // Create layout
        LayoutElementBuilders.Layout.Builder layoutBuilder = new LayoutElementBuilders.Layout.Builder();
        
        // Create a clickable modifier to launch the app when the tile is tapped
        ModifiersBuilders.Clickable clickable = new ModifiersBuilders.Clickable.Builder()
            .setId("launch_app")
            .build();
        
        // Create the layout based on glucose data
        if (dg != null) {
            // Check if we have actual glucose data or just a "No Data" message
            if (dg.unitized.equals("No Data")) {
                // Display the "No Data" message
                layoutBuilder.setRoot(
                    new LayoutElementBuilders.Column.Builder()
                        .setModifiers(new ModifiersBuilders.Modifiers.Builder()
                            .setClickable(clickable)
                            .setPadding(new ModifiersBuilders.Padding.Builder()
                                .setAll(DimensionBuilders.dp(8))
                                .build())
                            .build())
                        .addContent(
                            new LayoutElementBuilders.Text.Builder()
                                .setText(dg.unitized)
                                .setFontStyle(new LayoutElementBuilders.FontStyle.Builder()
                                    .setSize(DimensionBuilders.sp(24))
                                    .setColor(ColorBuilders.argb(Color.WHITE))
                                    .build())
                                .build())
                        .addContent(
                            new LayoutElementBuilders.Text.Builder()
                                .setText(dg.delta_name)
                                .setFontStyle(new LayoutElementBuilders.FontStyle.Builder()
                                    .setSize(DimensionBuilders.sp(16))
                                    .setColor(ColorBuilders.argb(Color.LTGRAY))
                                    .build())
                                .build())
                        .build()
                );
            } else {
                // Determine text color based on glucose level
                int textColor = Color.GREEN;
                if (dg.mgdl < 70) {
                    textColor = Color.RED;
                } else if (dg.mgdl > 180) {
                    textColor = Color.YELLOW;
                }
                
                // Create a column layout
                layoutBuilder.setRoot(
                    new LayoutElementBuilders.Column.Builder()
                        .setModifiers(new ModifiersBuilders.Modifiers.Builder()
                            .setClickable(clickable)
                            .setPadding(new ModifiersBuilders.Padding.Builder()
                                .setAll(DimensionBuilders.dp(8))
                                .build())
                            .build())
                        .addContent(
                            new LayoutElementBuilders.Text.Builder()
                                .setText(dg.mgdl + " " + dg.unitized)
                                .setFontStyle(new LayoutElementBuilders.FontStyle.Builder()
                                    .setSize(DimensionBuilders.sp(24))
                                    .setColor(ColorBuilders.argb(textColor))
                                    .build())
                                .build())
                        .addContent(
                            new LayoutElementBuilders.Text.Builder()
                                .setText(dg.delta_arrow)
                                .setFontStyle(new LayoutElementBuilders.FontStyle.Builder()
                                    .setSize(DimensionBuilders.sp(20))
                                    .setColor(ColorBuilders.argb(textColor))
                                    .build())
                                .build())
                        .addContent(
                            new LayoutElementBuilders.Text.Builder()
                                .setText(dg.delta_name)
                                .setFontStyle(new LayoutElementBuilders.FontStyle.Builder()
                                    .setSize(DimensionBuilders.sp(16))
                                    .setColor(ColorBuilders.argb(Color.WHITE))
                                    .build())
                                .build())
                        .addContent(
                            new LayoutElementBuilders.Text.Builder()
                                .setText(JoH.niceTimeScalar(dg.mssince) + " ago")
                                .setFontStyle(new LayoutElementBuilders.FontStyle.Builder()
                                    .setSize(DimensionBuilders.sp(14))
                                    .setColor(ColorBuilders.argb(Color.LTGRAY))
                                    .build())
                                .build())
                        .build()
                );
            }
        } else {
            // No data available (this should not happen now that we return a default DisplayGlucose)
            layoutBuilder.setRoot(
                new LayoutElementBuilders.Column.Builder()
                    .setModifiers(new ModifiersBuilders.Modifiers.Builder()
                        .setClickable(clickable)
                        .setPadding(new ModifiersBuilders.Padding.Builder()
                            .setAll(DimensionBuilders.dp(8))
                            .build())
                        .build())
                    .addContent(
                        new LayoutElementBuilders.Text.Builder()
                            .setText("No glucose data")
                            .setFontStyle(new LayoutElementBuilders.FontStyle.Builder()
                                .setSize(DimensionBuilders.sp(18))
                                .setColor(ColorBuilders.argb(Color.WHITE))
                                .build())
                            .build())
                    .build()
            );
        }
        
        return Futures.immediateFuture(
            new TileBuilders.Tile.Builder()
                .setResourcesVersion(RESOURCES_VERSION)
                .setTimeline(
                    new TimelineBuilders.Timeline.Builder()
                        .addTimelineEntry(
                            new TimelineBuilders.TimelineEntry.Builder()
                                .setLayout(layoutBuilder.build())
                                .build()
                        )
                        .build()
                )
                .setFreshnessIntervalMillis(TILE_REFRESH_INTERVAL_MILLIS)
                .build()
        );
    }

    @NonNull
    @Override
    protected ListenableFuture<ResourceBuilders.Resources> onResourcesRequest(@NonNull RequestBuilders.ResourcesRequest request) {
        return Futures.immediateFuture(
            new ResourceBuilders.Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .build()
        );
    }
}