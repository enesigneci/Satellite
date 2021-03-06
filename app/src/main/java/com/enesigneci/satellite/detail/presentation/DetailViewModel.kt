package com.enesigneci.satellite.detail.presentation

import android.text.Spanned
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enesigneci.satellite.R
import com.enesigneci.satellite.base.StringProvider
import com.enesigneci.satellite.detail.data.model.SatelliteDetail
import com.enesigneci.satellite.detail.data.model.SatelliteDetailUIModel
import com.enesigneci.satellite.detail.domain.DetailUseCase
import com.enesigneci.satellite.list.data.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val stringProvider: StringProvider,
    private val detailUseCase: DetailUseCase,
    savedStateHandle: SavedStateHandle
): ViewModel() {

    private val args = DetailFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val id = args.id

    private val _uiLiveData = MutableLiveData<Resource<SatelliteDetail>>()
    val uiLiveData: LiveData<Resource<SatelliteDetail>> = _uiLiveData

    private val _uiModelLiveData = MutableLiveData<SatelliteDetailUIModel>()
    val uiModelLiveData: LiveData<SatelliteDetailUIModel> = _uiModelLiveData

    private val _positionsLiveData = MutableLiveData<Spanned>()
    val positionsLiveData: LiveData<Spanned> = _positionsLiveData


    fun getSatelliteDetail() {
        viewModelScope.launch {
            _uiLiveData.postValue(Resource.Loading)
            with(detailUseCase.getSatelliteById(id)) {
                if (this?.id == null) {
                    _uiLiveData.postValue(Resource.Error(Exception(stringProvider.getString(R.string.couldnt_get_satellite_detail))))
                } else {
                    _uiLiveData.postValue(Resource.Success(this))
                    val satelliteDetailUIModel = SatelliteDetailUIModel(
                        args.name,
                        buildSpannedString {
                            bold {
                                append(stringProvider.getString(R.string.height_mass))
                            }
                            append("$height / $mass")
                        },
                        prepareDate(firstFlight),
                        prepareCost(costPerLaunch)
                    )
                    _uiModelLiveData.postValue(satelliteDetailUIModel)
                    while (true){
                        requestPositions(id.toString())
                        delay(3000)
                    }
                }
            }
        }
    }

    private fun prepareDate(date: String?): String {
        val inputDate = SimpleDateFormat(INPUT_DATE_PATTERN, Locale.getDefault()).parse(date)
        val outputDate = SimpleDateFormat(OUTPUT_DATE_PATTERN, Locale.getDefault()).format(inputDate)
        return outputDate.toString()
    }

    private fun prepareCost(cost: Int?): Spanned {
        return buildSpannedString {
            bold {
                append(stringProvider.getString(R.string.cost))
            }
            append(DecimalFormat(COST_PATTERN, DecimalFormatSymbols().apply { groupingSeparator = '.' }).format(cost))
        }
    }

    private fun requestPositions(id: String) {
        viewModelScope.launch {
            detailUseCase.getPositions(id)?.positions?.random()?.let {
                _positionsLiveData.postValue(
                    buildSpannedString {
                        bold {
                            append(stringProvider.getString(R.string.last_position))
                        }
                        append("(${it.posX},${it.posY})")
                    }
                )
            }
        }
    }

    companion object {
        const val INPUT_DATE_PATTERN = "yyyy-mm-dd"
        const val OUTPUT_DATE_PATTERN = "dd.mm.yyyy"
        const val COST_PATTERN = "#,###,###"
    }
}