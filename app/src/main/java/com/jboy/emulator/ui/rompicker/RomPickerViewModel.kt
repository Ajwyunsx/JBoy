package com.jboy.emulator.ui.rompicker

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jboy.emulator.data.RomInfo
import com.jboy.emulator.data.RomRepository
import com.jboy.emulator.data.RomScanState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RomPickerViewModel : ViewModel() {
    
    private lateinit var repository: RomRepository
    
    private val _scanState = MutableStateFlow<RomScanState>(RomScanState.Idle)
    val scanState: StateFlow<RomScanState> = _scanState.asStateFlow()
    
    private val _selectedRoms = MutableStateFlow<Set<RomInfo>>(emptySet())
    val selectedRoms: StateFlow<Set<RomInfo>> = _selectedRoms.asStateFlow()
    
    fun initRepository(context: Context) {
        if (!::repository.isInitialized) {
            repository = RomRepository(context.applicationContext)
        }
    }
    
    /**
     * 扫描文件夹
     */
    fun scanFolder(context: Context, folderUri: Uri) {
        initRepository(context)
        
        viewModelScope.launch {
            repository.scanRomFiles(folderUri).collect { state ->
                _scanState.value = state
            }
        }
    }
    
    /**
     * 导入多个ROM文件
     */
    fun importRoms(context: Context, uris: List<Uri>) {
        initRepository(context)

        viewModelScope.launch {
            repository.importRomUris(uris).collect { state ->
                _scanState.value = state
                if (state is RomScanState.Complete) {
                    _selectedRoms.value = state.roms.toSet()
                }
            }
        }
    }
    
    /**
     * 切换选中状态
     */
    fun toggleSelection(rom: RomInfo) {
        _selectedRoms.update { current ->
            if (rom in current) {
                current - rom
            } else {
                current + rom
            }
        }
    }
    
    /**
     * 清除所有选择
     */
    fun clearSelection() {
        _selectedRoms.value = emptySet()
    }
    
    /**
     * 全选
     */
    fun selectAll(roms: List<RomInfo>) {
        _selectedRoms.value = roms.toSet()
    }

    fun invertSelection(roms: List<RomInfo>) {
        _selectedRoms.update { current ->
            roms.filterNot { it in current }.toSet()
        }
    }
}
