package helper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.example.agriexpensett.localCycleUse;
import com.example.agriexpensett.cycleendpoint.model.Cycle;
import com.example.agriexpensett.cycleuseendpoint.model.CycleUse;
import com.example.agriexpensett.rpurchaseendpoint.model.RPurchase;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class DataManager {
	SQLiteDatabase db;
	DbHelper dbh;
	Context context;
	TransactionLog tL;
	public DataManager(Context context){
		dbh= new DbHelper(context);
		db=dbh.getReadableDatabase();
		this.context=context;
		tL=new TransactionLog(dbh,db);
	}
	public DataManager(Context context,SQLiteDatabase db,DbHelper dbh){
		this.dbh= dbh;
		this.db=db;
		this.context=context;
		tL=new TransactionLog(dbh,db);
	}

	public void insertCycle(int cropId, String landType, double landQty,long time){
		//insert into database
		int id=DbQuery.insertCycle(db, dbh, cropId, landType, landQty,tL,time);
		//insert into transaction table
		DbQuery.insertRedoLog(db, dbh, DbHelper.TABLE_CROPCYLE, id, "ins");
		
		//try insert into cloud
		CloudInterface c= new CloudInterface(context,db,dbh);// new CloudInterface(context);
		c.insertCycleC();
		//update database last updated time
	}
	
	//pass in the cycleId
	public void deleteCycle( int id){
		//delete from database
		DbQuery.deleteRecord(db, dbh, DbHelper.TABLE_CROPCYLE,id);
		//insert into transaction table
		DbQuery.insertRedoLog(db, dbh, DbHelper.TABLE_CROPCYLE, id,"del");
		//try delete from cloud
		CloudInterface c= new CloudInterface(context,db,dbh);//new CloudInterface(context);
		c.deleteCycle();
			//if successful
				//delete from transaction table
				//and update cloud's last updated time
		//update database last updated time
	}
	
	public void deleteCycleUse( int id){
		//delete from database
		DbQuery.deleteRecord(db, dbh, DbHelper.TABLE_CYCLE_RESOURCES,id);
		//insert into transaction table
		DbQuery.insertRedoLog(db, dbh, DbHelper.TABLE_CYCLE_RESOURCES, id,"del");
		//try delete from cloud
		CloudInterface c= new CloudInterface(context,db,dbh);//new CloudInterface(context);
		c.deleteCycleUse();
			//if successful
				//delete from transaction table
				//and update cloud's last updated time
		//update database last updated time
	}

	public void deletePurchase( int id){
		//delete from database
		DbQuery.deleteRecord(db, dbh, DbHelper.TABLE_RESOURCE_PURCHASES,id);
		//insert into transaction table
		DbQuery.insertRedoLog(db, dbh, DbHelper.TABLE_RESOURCE_PURCHASES, id,"del");
		//try delete from cloud
		CloudInterface c= new CloudInterface(context,db,dbh);//new CloudInterface(context);
		c.deletePurchase();
			//if successful
				//delete from transaction table
				//and update cloud's last updated time
		//update database last updated time
	}
	
	public void insertCycleUse(int cycleId, int resPurchaseId, double qty,String type){
		
		//insert into database
		int id=DbQuery.insertResourceUse(db, dbh, cycleId, type, resPurchaseId, qty, tL);
		//insert into redo log table
		DbQuery.insertRedoLog(db, dbh, DbHelper.TABLE_CYCLE_RESOURCES, id, "ins");
		//try to insert into cloud
		CloudInterface c= new CloudInterface(context,db,dbh);//new CloudInterface(context);
		c.insertCycleUseC();
	}
	
	public void insertPurchase( int resourceId, String quantifier, double qty,String type,double cost){
		
		//insert into database
		int id=DbQuery.insertResourceExp(db, dbh, type, resourceId, quantifier, qty, cost, tL);
		//DbQuery.insertResourceExp(db, dbh, type, resourceId, quantifier, qty, cost, tl)
		//insert into redo log table
		DbQuery.insertRedoLog(db, dbh, DbHelper.TABLE_RESOURCE_PURCHASES, id, "ins");
		//try to insert into cloud
		CloudInterface c= new CloudInterface(context,db,dbh);//new CloudInterface(context);
		c.insertPurchase();
		//c.
	}
	public void update(){
		//check time-updated in cloud
		long locaupdated;
		//check time-updated in local
		long cloudupdated;
		//if both are the same 
			//then do nothing
		
		//if cloud is more recent
			updateLocal();
		
		//if local is more recent
			
	}
	private void updateLocal(){
		//since data is being pulled from a network we are using an async task
		//since the async task is being run on a diff thread asynchronously
			//we must do the work there because we wont know when it returns
		CloudInterface c= new CloudInterface(context);
		//get all recrods from cloud (FIRST)
			//-- get lists of all object kinds in database
			//c.updateLocal();
		tL.updateLocal(0);
		
		System.out.println("meh");
		//delete all records from local db   
			//-- drop tables then recreate
		//write lists to local database
			//-- iterator and insert
		//set local db last updated to cloud's last updated
	}
	private void updateCloud(){ 
			//-- put all inserts in the redo log
			//-- call the insert method for each kind
		//set cloud last updated to local db's last updated
	}
	public void updatePurchase(int id,double amt){
		TransactionLog tl=new TransactionLog(dbh, db);
		tl.insertTransLog(DbHelper.TABLE_RESOURCE_PURCHASES, id, "up");
		String code="update "+DbHelper.TABLE_RESOURCE_PURCHASES+
				" set "+DbHelper.RESOURCE_PURCHASE_REMAINING+"="+amt+
				" where "+DbHelper.RESOURCE_PURCHASE_ID+"="+id;
		db.execSQL(code);
		DbQuery.insertRedoLog(db, dbh, DbHelper.TABLE_RESOURCE_PURCHASES, id, "up");
	}
	public void updateCycleSpent(int id,double amt){
		String strFilter = DbHelper.CROPCYCLE_ID+"="+id;
		ContentValues args = new ContentValues();
		args.put(DbHelper.CROPCYCLE_TOTALSPENT,amt);
		db.update(DbHelper.TABLE_CROPCYLE, args, strFilter, null);
		//update the cloud
		DbQuery.insertRedoLog(db, dbh, DbHelper.TABLE_CROPCYLE, id, "up");
		//record in transaction log
		TransactionLog tl=new TransactionLog(dbh, db);
		tl.insertTransLog(DbHelper.TABLE_RESOURCE_PURCHASES, id, "up");
	}
	
	//------------------------------------------------------------------fixed deletes
	public void delPurchase(int pId){
		String code="select * from "+DbHelper.TABLE_CYCLE_RESOURCES+" where "
				+DbHelper.CYCLE_RESOURCE_PURCHASE_ID+"="+pId;
		Cursor cursor=db.rawQuery(code, null);
		if(cursor.getCount()<1)
			return;
		
		while(cursor.moveToNext()){
			int cId=cursor.getInt(cursor.getColumnIndex(DbHelper.CYCLE_RESOURCE_CYCLEID));
			
			try{
				db.delete(DbHelper.TABLE_CYCLE_RESOURCES, DbHelper.CYCLE_RESOURCE_CYCLEID+"="+cId, null);
				db.delete(DbHelper.TABLE_CROPCYLE, DbHelper.CROPCYCLE_ID+"="+cId, null);
			}catch(Exception e){
				e.printStackTrace();
			}
			System.out.println("del");
		}
		db.delete(DbHelper.TABLE_RESOURCE_PURCHASES, DbHelper.RESOURCE_PURCHASE_ID+"="+pId, null);
	}
	
	public void delResource(int resId){
		String code="select * from "+DbHelper.TABLE_RESOURCE_PURCHASES+" where "
				+DbHelper.RESOURCE_PURCHASE_RESID+"="+resId;
		Cursor cursor=db.rawQuery(code, null);
		if(cursor.getCount()<1)
			return;
		while(cursor.moveToNext()){
			int pId=cursor.getInt(cursor.getColumnIndex(DbHelper.RESOURCE_PURCHASE_ID));
			this.delPurchase(pId);
		}
		db.delete(DbHelper.TABLE_RESOURCES, DbHelper.RESOURCES_ID+"="+resId, null);
	}
	
	
}
