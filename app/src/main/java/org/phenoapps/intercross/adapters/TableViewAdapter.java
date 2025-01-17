package org.phenoapps.intercross.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.evrencoskun.tableview.adapter.AbstractTableAdapter;
import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractSorterViewHolder;
import com.evrencoskun.tableview.adapter.recyclerview.holder.AbstractViewHolder;

import org.jetbrains.annotations.NotNull;
import org.phenoapps.intercross.R;
import org.phenoapps.intercross.fragments.CrossTrackerFragment;

/**
 * https://github.com/evrencoskun/TableView/wiki
 */
public class TableViewAdapter extends AbstractTableAdapter<CrossTrackerFragment.CellData, CrossTrackerFragment.CellData, CrossTrackerFragment.CellData> {

    static class CellViewHolder extends AbstractSorterViewHolder {

        final TextView textView;
        final ImageView imageView;
        final LinearLayout linearLayout;

        public CellViewHolder(View itemView) {
            super(itemView);

            linearLayout = itemView.findViewById(R.id.list_item_table_cell_container);
            imageView = itemView.findViewById(R.id.list_item_table_cell_iv);
            textView = itemView.findViewById(R.id.list_item_table_cell_tv);
        }
    }

    static class RowHeaderViewHolder extends AbstractViewHolder {

        final ImageView imageView;

        public RowHeaderViewHolder(@NonNull View itemView) {
            super(itemView);

            imageView = itemView.findViewById(R.id.list_item_table_checkmark_iv);
            imageView.setVisibility(View.GONE);
        }
    }

     /**
      * This is where you create your custom Cell ViewHolder. This method is called when Cell
      * RecyclerView of the TableView needs a new RecyclerView.ViewHolder of the given type to
      * represent an item.
      *
      * @param viewType : This value comes from #getCellItemViewType method to support different type
      *                 of viewHolder as a Cell item.
      *
      * @see #getCellItemViewType(int);
      */
     @NotNull
     @Override
     public AbstractViewHolder onCreateCellViewHolder(ViewGroup parent, int viewType) {
         // Get cell xml layout 
         View layout = LayoutInflater.from(parent.getContext())
              .inflate(R.layout.list_item_table_cell, parent, false);
         // Create a Custom ViewHolder for a Cell item.
         return new CellViewHolder(layout);
     }
 
     /**
      * That is where you set Cell View Model data to your custom Cell ViewHolder. This method is
      * Called by Cell RecyclerView of the TableView to display the data at the specified position.
      * This method gives you everything you need about a cell item.
      *
      * @param holder       : This is one of your cell ViewHolders that was created on
      *                     ```onCreateCellViewHolder``` method. In this example, we have created
      *                     "MyCellViewHolder" holder.
      * @param cellItemModel     : This is the cell view model located on this X and Y position. In this
      *                     example, the model class is "Cell".
      * @param columnPosition : This is the X (Column) position of the cell item.
      * @param rowPosition : This is the Y (Row) position of the cell item.
      *
      * @see #onCreateCellViewHolder(ViewGroup, int);
      */
     @Override
     public void onBindCellViewHolder(@NotNull AbstractViewHolder holder, CrossTrackerFragment.CellData cellItemModel, int
             columnPosition, int rowPosition) {

         // Get the holder to update cell item text
         CellViewHolder viewHolder = (CellViewHolder) holder;

         if (cellItemModel != null) {
             viewHolder.textView.setText(cellItemModel.getText());
             if (cellItemModel.getComplete()) {
                 viewHolder.textView.setVisibility(View.INVISIBLE);
                 viewHolder.imageView.setVisibility(View.VISIBLE);
             } else {
                 viewHolder.textView.setVisibility(View.VISIBLE);
                 viewHolder.imageView.setVisibility(View.INVISIBLE);
             }
         }

         // If your TableView should have auto resize for cells & columns.
         // Then you should consider the below lines. Otherwise, you can ignore them.
 
         // It is necessary to remeasure itself.
         viewHolder.linearLayout.getLayoutParams().width = LinearLayout.LayoutParams.WRAP_CONTENT;
         //viewHolder.imageView.requestLayout();
         //viewHolder.textView.requestLayout();
     }

     /**
      * This is where you create your custom Column Header ViewHolder. This method is called when
      * Column Header RecyclerView of the TableView needs a new RecyclerView.ViewHolder of the given
      * type to represent an item.
      *
      * @param viewType : This value comes from "getColumnHeaderItemViewType" method to support
      *                 different type of viewHolder as a Column Header item.
      *
      * @see #getColumnHeaderItemViewType(int);
      */
     @NotNull
     @Override
     public AbstractViewHolder onCreateColumnHeaderViewHolder(ViewGroup parent, int viewType) {
 
         // Get Column Header xml Layout
         View layout = LayoutInflater.from(parent.getContext())
                 .inflate(R.layout.list_item_table_cell, parent, false);
 
         // Create a ColumnHeader ViewHolder
         return new CellViewHolder(layout);
     }
 
     /**
      * That is where you set Column Header View Model data to your custom Column Header ViewHolder.
      * This method is Called by ColumnHeader RecyclerView of the TableView to display the data at
      * the specified position. This method gives you everything you need about a column header
      * item.
      *
      * @param holder   : This is one of your column header ViewHolders that was created on
      *                 ```onCreateColumnHeaderViewHolder``` method. In this example we have created
      *                 "MyColumnHeaderViewHolder" holder.
      * @param columnHeaderItemModel : This is the column header view model located on this X position. In this
      *                 example, the model class is "ColumnHeader".
      * @param position : This is the X (Column) position of the column header item.
      *
      * @see #onCreateColumnHeaderViewHolder(ViewGroup, int) ;
      */
     @Override
     public void onBindColumnHeaderViewHolder(@NotNull AbstractViewHolder holder,
                                              CrossTrackerFragment.CellData columnHeaderItemModel,
                                              int position) {

         // Get the holder to update cell item text
         CellViewHolder columnHeaderViewHolder = (CellViewHolder) holder;

         if (columnHeaderItemModel != null) {
             columnHeaderViewHolder.textView.setText(columnHeaderItemModel.getText());
         }

         // If your TableView should have auto resize for cells & columns.
         // Then you should consider the below lines. Otherwise, you can ignore them.

         // It is necessary to remeasure itself.
        // columnHeaderViewHolder.container.getLayoutParams().width = LinearLayout.LayoutParams.WRAP_CONTENT;
         columnHeaderViewHolder.textView.requestLayout();
     }
     
     /**
      * This is where you create your custom Row Header ViewHolder. This method is called when
      * Row Header RecyclerView of the TableView needs a new RecyclerView.ViewHolder of the given
      * type to represent an item.
      *
      * @param viewType : This value comes from "getRowHeaderItemViewType" method to support
      *                 different type of viewHolder as a row Header item.
      *
      * @see #getRowHeaderItemViewType(int);
      */
     @NotNull
     @Override
     public AbstractViewHolder onCreateRowHeaderViewHolder(ViewGroup parent, int viewType) {
 
         // Get Row Header xml Layout
         View layout = LayoutInflater.from(parent.getContext())
                 .inflate(R.layout.list_item_table_checkmark, parent, false);
 
         // Create a Row Header ViewHolder
         return new RowHeaderViewHolder(layout);
     }
 
 
     /**
      * That is where you set Row Header View Model data to your custom Row Header ViewHolder. This
      * method is Called by RowHeader RecyclerView of the TableView to display the data at the
      * specified position. This method gives you everything you need about a row header item.
      *
      * @param holder   : This is one of your row header ViewHolders that was created on
      *                 ```onCreateRowHeaderViewHolder``` method. In this example, we have created
      *                 "MyRowHeaderViewHolder" holder.
      * @param rowHeaderItemModel : This is the row header view model located on this Y position. In this
      *                 example, the model class is "RowHeader".
      * @param position : This is the Y (row) position of the row header item.
      *
      * @see #onCreateRowHeaderViewHolder(ViewGroup, int) ;
      */
     @Override
     public void onBindRowHeaderViewHolder(@NotNull AbstractViewHolder holder, CrossTrackerFragment.CellData rowHeaderItemModel, int
             position) {

         // Get the holder to update row header item text
         RowHeaderViewHolder rowHeaderViewHolder = (RowHeaderViewHolder) holder;

         if (rowHeaderItemModel != null) {
             if (rowHeaderItemModel.getComplete()) {
                 rowHeaderViewHolder.itemView.setVisibility(View.VISIBLE);
             } else {
                 rowHeaderViewHolder.itemView.setVisibility(View.INVISIBLE);
             }
         }
     }

     @NotNull
     @Override
     public View onCreateCornerView(ViewGroup parent) {
         // Get Corner xml layout
         return LayoutInflater.from(parent.getContext())
              .inflate(R.layout.list_item_corner, parent, false);
     }
 
     @Override
     public int getColumnHeaderItemViewType(int columnPosition) {
         // The unique ID for this type of column header item
         // If you have different items for Cell View by X (Column) position, 
         // then you should fill this method to be able create different 
         // type of ColumnViewHolder on "onCreateColumnViewHolder"
         return 0;
     }
 
     @Override
     public int getRowHeaderItemViewType(int rowPosition) {
         // The unique ID for this type of row header item
         // If you have different items for Row Header View by Y (Row) position, 
         // then you should fill this method to be able create different 
         // type of RowHeaderViewHolder on "onCreateRowHeaderViewHolder"
         return 0;
     }
 
     @Override
     public int getCellItemViewType(int columnPosition) {
         // The unique ID for this type of cell item
         // If you have different items for Cell View by X (Column) position, 
         // then you should fill this method to be able create different 
         // type of CellViewHolder on "onCreateCellViewHolder"
         return 0;
     }
 }