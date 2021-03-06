// This file is part of Weight Chart.
// Copyright 2010 Fredrik Portstrom
//
// Weight Chart is free software: you can redistribute it and/or
// modify it under the terms of the GNU General Public License as
// published by the Free Software Foundation, either version 3 of the
// License, or (at your option) any later version.
//
// Weight Chart is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with Weight Chart. If not, see
// <http://www.gnu.org/licenses/>.

package edu.ucla.cs.nopainnogame.weightchart;

import java.util.GregorianCalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.Toast;
import edu.ucla.cs.nopainnogame.R;

public class ChartActivity extends Activity {
    private ChartDraw mDraw;
    private Database mDatabase;
    private GestureDetector mGestureDetector;
    private HeightDialog mHeightDialog;
    private Toast mToast;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	setContentView(R.layout.chart);
	mDatabase = new Database(this);
	mDraw = new ChartDraw(ChartActivity.this, mDatabase, new GregorianCalendar());
	((ChartView)findViewById(R.id.chart)).setChartDraw(mDraw);
	mGestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		    if (distanceX != 0) {
			mDraw.mScrollX -= distanceX;
			invalidate();
		    }
		    return true;
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
		    if (mToast != null) {
			mToast.cancel();
			mToast = null;
		    }

		    openOptionsMenu();
		    return true;
		}
	    });
	mGestureDetector.setIsLongpressEnabled(false);

	if (PreferenceManager.getDefaultSharedPreferences(this).getString("weight_unit", null) == null) {
	    mHeightDialog = new HeightDialog(this) {
		    protected void done() {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			mToast = Toast.makeText(ChartActivity.this, R.string.tap_to_open_menu, Toast.LENGTH_LONG);
			//TODO: uncomment this line when tapping is enabled
			//mToast.show();
		    }
		};
	    showDialog(0);
	} else {
	    getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}
    }

    @Override
    protected void onDestroy() {
	super.onPause();
	mDatabase.close();

	if (mToast != null) {
	    mToast.cancel();
	    mToast = null;
	}
    }

    @Override
    protected void onResume() {
	super.onResume();
	mDraw.loadPreferences(this);
	invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
	return mGestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	getMenuInflater().inflate(R.menu.chart, menu);
	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	int id = item.getItemId();

	if (id == R.id.entries) {
            startActivity(new Intent(this, EntryListActivity.class));
	} else if (id == R.id.view_7_days) {
	    setDays(7);
	} else if (id == R.id.view_14_days) {
	    setDays(14);
	} else if (id == R.id.view_21_days) {
	    setDays(21);
	} else if (id == R.id.view_30_days) {
	    setDays(30);
	} else if (id == R.id.view_60_days) {
	    setDays(60);
	} else if (id == R.id.view_90_days) {
	    setDays(90);
        } else if (id == R.id.legend) {
            startActivity(new Intent(this, LegendActivity.class));
	} else if (!new FileCommands(this, mDatabase) {
		@Override
		protected void fillData() {
		    invalidate();
		}
	    }.onOptionsItemSelected(id)) {
	    return super.onOptionsItemSelected(item);
	}

	return true;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
	return id > 0 ? mHeightDialog.createDialog(id) : createWeightUnitDialog();
    }

    private Dialog createWeightUnitDialog() {
	AlertDialog.Builder builder = new AlertDialog.Builder(this);
	builder.setCancelable(false);
	builder.setTitle(R.string.weight_unit);
	builder.setSingleChoiceItems(R.array.weight_unit_labels, -1, new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int item) {
		    dialog.dismiss();
		    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(ChartActivity.this).edit();
		    editor.putString("weight_unit", getResources().getStringArray(R.array.weight_unit_values)[item]);
		    editor.commit();
		    showDialog(1);
		}
	    });
	return builder.create();
    }

    private void invalidate() {
	((ChartView)findViewById(R.id.chart)).invalidate();
    }

    private void setDays(int days) {
	mDraw.mScrollX *= (float)mDraw.mDays / days;
	mDraw.mDays = days;
	invalidate();
    }
}
