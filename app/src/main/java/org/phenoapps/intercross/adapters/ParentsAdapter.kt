package org.phenoapps.intercross.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.databinding.DataBindingUtil
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.sqlite.throwSQLiteException
import org.phenoapps.intercross.R
import org.phenoapps.intercross.data.models.BaseParent
import org.phenoapps.intercross.data.models.Parent
import org.phenoapps.intercross.data.models.PollenGroup
import org.phenoapps.intercross.data.viewmodels.ParentsListViewModel
import org.phenoapps.intercross.data.viewmodels.PollenGroupListViewModel
import org.phenoapps.intercross.databinding.ListItemSelectableParentRowBinding

class ParentsAdapter(private val listModel: ParentsListViewModel,
                    private val groupModel: PollenGroupListViewModel,
                    private val crossCountProvider: (BaseParent) -> Int = { 0 })
    : ListAdapter<BaseParent, ParentsAdapter.ViewHolder>(BaseParent.Companion.DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(

                DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.list_item_selectable_parent_row, parent, false
                )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        getItem(position).let { item ->

            with(holder) {

                bind(item)
            }
        }
    }

    inner class ViewHolder(private val binding: ListItemSelectableParentRowBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(p: BaseParent) {

            with(binding) {
                val context = root.context
                val isPollenGroup = p is PollenGroup
                val crossCount = crossCountProvider(p)
                val isSelected = when (p) {
                    is Parent -> p.selected
                    is PollenGroup -> p.selected
                    else -> false
                }

                name = when (p) {
                    is Parent -> p.name
                    is PollenGroup -> p.name
                    else -> ""
                }

                code = when (p) {
                    is Parent -> p.codeId
                    is PollenGroup -> p.codeId
                    else -> null
                }

                checked = isSelected

                doneCheckBox.isChecked = isSelected
                pollenGroupChip.isVisible = isPollenGroup
                pollenGroupChip.text = context.getString(R.string.parent_pollen_group_chip)
                crossCountChip.text = crossCount.toString()

                crossCountChip.visibility = if (crossCount > 0) View.VISIBLE else View.GONE

                parentTypeChip.chipIcon = when((p as? Parent)?.sex) {
                    0 -> AppCompatResources.getDrawable(context, R.drawable.ic_female_black_24dp)
                    else -> AppCompatResources.getDrawable(context, R.drawable.ic_male_black_24dp)
                }

                parentCard.setOnClickListener {
                    doneCheckBox.isChecked = !doneCheckBox.isChecked

                    if (p is Parent) {

                        listModel.update(p.apply {

                            selected = doneCheckBox.isChecked

                        })
                    } else if (p is PollenGroup) {

                        groupModel.update(p.apply {

                            selected = doneCheckBox.isChecked

                        })
                    }
                }

                executePendingBindings()
            }
        }
    }
}