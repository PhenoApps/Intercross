package org.phenoapps.intercross.fragments.wish_factory

import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import org.phenoapps.intercross.activities.MainActivity
import org.phenoapps.intercross.R
import org.phenoapps.intercross.databinding.FragmentBarcodeScanBinding
import org.phenoapps.intercross.fragments.IntercrossBaseFragment

class BarcodeScanFragment: IntercrossBaseFragment<FragmentBarcodeScanBinding>(R.layout.fragment_barcode_scan) {

    private lateinit var mBarcodeScanner: DecoratedBarcodeView

    private lateinit var mCallback: BarcodeCallback

    private var lastText: String? = null

    private val checkCamPermissions by lazy {

        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->

            if (granted) {

                mBinding.setupBarcodeScanner()

            }
        }
    }

    private fun FragmentBarcodeScanBinding.setupBarcodeScanner() {

        mCallback = object : BarcodeCallback {

            override fun barcodeResult(result: BarcodeResult) {

                if (result.text == null || !isAdded) return

                lastText = result.text

                zxingBarcodeScanner.statusView.text = getString(R.string.zxing_scan_mode_single)

                zxingBarcodeScanner.setStatusText(getString(R.string.zxing_status_single))

                //navigate back to the female or male fragment with the barcode
                with(findNavController()) {
                    navigate(when (previousBackStackEntry?.destination?.id) {
                        R.id.wf_female_fragment -> {
                            R.id.wf_female_fragment
                        }
                        else -> {
                            R.id.wf_male_fragment
                        }
                    }, bundleOf("barcode" to result.text,
                        "name" to arguments?.getString("femaleName"),
                        "id" to arguments?.getString("femaleId")))
                }

            }

            override fun possibleResultPoints(resultPoints: List<ResultPoint>) {

            }

        }

        mBarcodeScanner = zxingBarcodeScanner

        mBarcodeScanner.barcodeView.apply {

            cameraSettings.isContinuousFocusEnabled = true

            cameraSettings.isAutoTorchEnabled = true

            cameraSettings.isAutoFocusEnabled = true

            cameraSettings.isBarcodeSceneModeEnabled = true

            decodeSingle(mCallback)
        }
    }


    override fun FragmentBarcodeScanBinding.afterCreateView() {

        (activity as? MainActivity)?.applyBottomInsets(root)

        checkCamPermissions.launch(Manifest.permission.CAMERA)

        val singleScanString = getString(R.string.single_scan_description)

        arguments?.let {

            zxingBarcodeScanner.setStatusText(singleScanString)
        }

        setupBarcodeScanner()
    }

    override fun onResume() {
        super.onResume()
        mBarcodeScanner.resume()
        (activity as MainActivity).setBackButtonToolbar()
        (activity as AppCompatActivity).supportActionBar?.show()
    }

    override fun onPause() {
        super.onPause()

        mBarcodeScanner.pause()
    }
}