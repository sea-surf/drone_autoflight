package com.dji.mavic3mauto

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dji.open.pilot.client.manager.ServerState
import dji.sdk.keyvalue.key.BatteryKey
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.value.common.EmptyMsg
import dji.sdk.keyvalue.value.flightcontroller.FlightCoordinateSystem
import dji.sdk.keyvalue.value.flightcontroller.RollPitchControlMode
import dji.sdk.keyvalue.value.flightcontroller.VerticalControlMode
import dji.sdk.keyvalue.value.flightcontroller.VirtualStickFlightControlParam
import dji.sdk.keyvalue.value.flightcontroller.YawControlMode
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.KeyManager
import dji.v5.manager.SDKManager
import dji.v5.manager.aircraft.virtualstick.VirtualStickManager
import dji.v5.manager.interfaces.SDKManagerCallback
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.hypot

class MainActivity : AppCompatActivity() {

    private lateinit var tvSdkStatus: TextView
    private lateinit var tvConnection: TextView
    private lateinit var tvAltitude: TextView
    private lateinit var tvSpeed: TextView
    private lateinit var tvBattery: TextView
    private lateinit var tvSatellites: TextView
    private lateinit var tvFlightMode: TextView
    private lateinit var tvMissionStatus: TextView
    private lateinit var btnStartMission: Button
    private lateinit var btnLand: Button
    private lateinit var btnEmergencyStop: Button

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var missionJob: Job? = null
    private var currentAltitude: Double = 0.0
    private var isDroneConnected = false

    private val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvSdkStatus = findViewById(R.id.tvSdkStatus)
        tvConnection = findViewById(R.id.tvConnection)
        tvAltitude = findViewById(R.id.tvAltitude)
        tvSpeed = findViewById(R.id.tvSpeed)
        tvBattery = findViewById(R.id.tvBattery)
        tvSatellites = findViewById(R.id.tvSatellites)
        tvFlightMode = findViewById(R.id.tvFlightMode)
        tvMissionStatus = findViewById(R.id.tvMissionStatus)
        btnStartMission = findViewById(R.id.btnStartMission)
        btnLand = findViewById(R.id.btnLand)
        btnEmergencyStop = findViewById(R.id.btnEmergencyStop)

        btnStartMission.setOnClickListener { startAutonomousMission() }
        btnLand.setOnClickListener { triggerAutoLanding() }
        btnEmergencyStop.setOnClickListener { emergencyStop() }

        checkPermissionsAndInitSDK()
    }

    private fun checkPermissionsAndInitSDK() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1001)
        } else {
            initDJISDK()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            initDJISDK()
        }
    }

    private fun initDJISDK() {
        tvSdkStatus.text = "SDK: Registrando aplicativo..."
        SDKManager.getInstance().init(this, object : SDKManagerCallback {
            override fun onRegisterSuccess() {
                runOnUiThread {
                    tvSdkStatus.text = "SDK: Registrado com sucesso!"
                    logStatus("SDK DJI V5 registrado. Conecte o Mavic 3M ao controle.")
                }
            }

            override fun onRegisterFailure(error: IDJIError?) {
                runOnUiThread {
                    tvSdkStatus.text = "SDK: Erro no registro (${error?.description()})"
                    logStatus("ERRO ao registrar SDK: ${error?.description()}")
                }
            }

            override fun onProductConnect(productId: Int) {
                runOnUiThread {
                    isDroneConnected = true
                    tvConnection.text = "Drone: Conectado"
                    tvConnection.setTextColor(0xFF81C784.toInt())
                    btnStartMission.isEnabled = true
                    btnLand.isEnabled = true
                    logStatus("Mavic 3M conectado! Pronto para missão.")
                    startTelemetryListeners()
                }
            }

            override fun onProductDisconnect(productId: Int) {
                runOnUiThread {
                    isDroneConnected = false
                    tvConnection.text = "Drone: Desconectado"
                    tvConnection.setTextColor(0xFFFF6B6B.toInt())
                    btnStartMission.isEnabled = false
                    btnLand.isEnabled = false
                    logStatus("Drone desconectado.")
                }
            }

            override fun onProductChanged(productId: Int) {}
            override fun onInitProcess(event: dji.v5.common.register.DJISDKInitEvent?, totalProcess: Int) {}
            override fun onDatabaseDownloadProgress(current: Long, total: Long) {}
            override fun onRemoteServerStateUpdated(serverState: ServerState?) {
                TODO("Not yet implemented")
            }

            override fun onRemoteServerCompatibleError(error: IDJIError?) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun startTelemetryListeners() {
        KeyManager.getInstance().listen(
            KeyTools.createKey(FlightControllerKey.KeyAltitude),
            this
        ) { _, newValue ->
            if (newValue != null) {
                currentAltitude = newValue
                runOnUiThread {
                    tvAltitude.text = String.format("Altura: %.2f m", currentAltitude)
                }
            }
        }

        KeyManager.getInstance().listen(
            KeyTools.createKey(FlightControllerKey.KeyAircraftVelocity),
            this
        ) { _, vel ->
            if (vel != null) {
                val horizSpeed = hypot(vel.x, vel.y)
                runOnUiThread {
                    tvSpeed.text = String.format("Velocidade: %.2f m/s (V: %.2f)", horizSpeed, vel.z)
                }
            }
        }

        KeyManager.getInstance().listen(
            KeyTools.createKey(BatteryKey.KeyChargeRemainingInPercent),
            this
        ) { _, newValue ->
            if (newValue != null) {
                runOnUiThread {
                    tvBattery.text = "Bateria: $newValue%"
                }
            }
        }

        KeyManager.getInstance().listen(
            KeyTools.createKey(FlightControllerKey.KeyGPSSatelliteCount),
            this
        ) { _, sats ->
            if (sats != null) {
                runOnUiThread {
                    tvSatellites.text = "GPS: $sats sats"
                }
            }
        }

        KeyManager.getInstance().listen(
            KeyTools.createKey(FlightControllerKey.KeyFlightMode),
            this
        ) { _, mode ->
            if (mode != null) {
                runOnUiThread {
                    tvFlightMode.text = "Modo: ${mode.name}"
                }
            }
        }
    }

    private fun startAutonomousMission() {
        if (!isDroneConnected) return
        if (missionJob?.isActive == true) {
            logStatus("Missão já está em andamento.")
            return
        }

        btnStartMission.isEnabled = false
        missionJob = scope.launch {
            try {
                logStatus("=== INICIANDO MISSÃO AUTÔNOMA ===")

                // 1. Decolar
                logStatus("1/6: Decolando...")
                performActionSuspend(FlightControllerKey.KeyStartTakeoff)
                delay(4500)

                // 2. Habilitar Virtual Stick (Modo Avançado)
                logStatus("2/6: Habilitando Virtual Stick...")
                enableVirtualStickSuspend()
                VirtualStickManager.getInstance().setVirtualStickAdvancedModeEnabled(true)

                // 3. Subir para 3.0 metros
                logStatus("3/6: Subindo para 3.0 m...")
                val ascendStart = System.currentTimeMillis()
                while (currentAltitude < 2.85 && System.currentTimeMillis() - ascendStart < 10000) {
                    sendVirtualStick(pitch = 0.0, roll = 0.0, yaw = 0.0, verticalTargetM = 3.0)
                    delay(50)
                }
                // Estabilizar a 3.0m
                sendHoverFor(1500, targetAltM = 3.0)

                // 4. Ir para frente 2.0 metros (0.5 m/s por 4 segundos)
                logStatus("4/6: Avançando 2.0 m...")
                sendVelocityFor(pitchMs = 0.5, durationMs = 4000, targetAltM = 3.0)
                sendHoverFor(1500, targetAltM = 3.0)

                // 5. Voltar 2.0 metros (-0.5 m/s por 4 segundos)
                logStatus("5/6: Retornando 2.0 m...")
                sendVelocityFor(pitchMs = -0.5, durationMs = 4000, targetAltM = 3.0)
                sendHoverFor(1000, targetAltM = 3.0)

                // 6. Desabilitar Virtual Stick e Pousar
                logStatus("6/6: Desabilitando Virtual Stick e pousando...")
                disableVirtualStickSuspend()
                performActionSuspend(FlightControllerKey.KeyStartAutoLanding)
                logStatus("=== MISSÃO CONCLUÍDA COM SUCESSO ===")
            } catch (e: CancellationException) {
                logStatus("Missão cancelada pelo operador.")
            } catch (e: Exception) {
                logStatus("ERRO na missão: ${e.message}")
                safeDisableVirtualStick()
            } finally {
                btnStartMission.isEnabled = isDroneConnected
            }
        }
    }

    private suspend fun sendVelocityFor(pitchMs: Double, durationMs: Long, targetAltM: Double) {
        val steps = (durationMs / 50).toInt()
        repeat(steps) {
            sendVirtualStick(pitch = pitchMs, roll = 0.0, yaw = 0.0, verticalTargetM = targetAltM)
            delay(50)
        }
    }

    private suspend fun sendHoverFor(durationMs: Long, targetAltM: Double) {
        val steps = (durationMs / 50).toInt()
        repeat(steps) {
            sendVirtualStick(pitch = 0.0, roll = 0.0, yaw = 0.0, verticalTargetM = targetAltM)
            delay(50)
        }
    }

    private fun sendVirtualStick(pitch: Double, roll: Double, yaw: Double, verticalTargetM: Double) {
        val param = VirtualStickFlightControlParam().apply {
            this.pitch = pitch
            this.roll = roll
            this.yaw = yaw
            this.verticalThrottle = verticalTargetM
            this.verticalControlMode = VerticalControlMode.POSITION
            this.rollPitchControlMode = RollPitchControlMode.VELOCITY
            this.yawControlMode = YawControlMode.ANGULAR_VELOCITY
            this.rollPitchCoordinateSystem = FlightCoordinateSystem.BODY
        }
        VirtualStickManager.getInstance().sendVirtualStickAdvancedParam(param)
    }

    private fun emergencyStop() {
        missionJob?.cancel()
        safeDisableVirtualStick()
        logStatus("PARADA DE EMERGÊNCIA! Virtual Stick desativado — drone em pairagem manual.")
        btnStartMission.isEnabled = isDroneConnected
    }

    private fun triggerAutoLanding() {
        missionJob?.cancel()
        safeDisableVirtualStick()
        KeyManager.getInstance().performAction(
            KeyTools.createKey(FlightControllerKey.KeyStartAutoLanding),
            object : CommonCallbacks.CompletionCallbackWithParam<EmptyMsg> {
                override fun onSuccess(t: EmptyMsg?) {
                    runOnUiThread { logStatus("Pouso automático iniciado.") }
                }

                override fun onFailure(error: IDJIError) {
                    runOnUiThread { logStatus("Erro ao iniciar pouso: ${error.description()}") }
                }
            }
        )
    }

    private fun safeDisableVirtualStick() {
        VirtualStickManager.getInstance().disableVirtualStick(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {}
            override fun onFailure(error: IDJIError) {}
        })
    }

    private suspend fun enableVirtualStickSuspend() = suspendCancellableCoroutine<Unit> { cont ->
        VirtualStickManager.getInstance().enableVirtualStick(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                if (cont.isActive) cont.resume(Unit)
            }

            override fun onFailure(error: IDJIError) {
                if (cont.isActive) cont.resumeWithException(RuntimeException(error.description()))
            }
        })
    }

    private suspend fun disableVirtualStickSuspend() = suspendCancellableCoroutine<Unit> { cont ->
        VirtualStickManager.getInstance().disableVirtualStick(object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() {
                if (cont.isActive) cont.resume(Unit)
            }

            override fun onFailure(error: IDJIError) {
                if (cont.isActive) cont.resumeWithException(RuntimeException(error.description()))
            }
        })
    }

    private suspend fun performActionSuspend(key: dji.sdk.keyvalue.key.DJIActionKeyInfo<EmptyMsg, EmptyMsg>) =
        suspendCancellableCoroutine<Unit> { cont ->
            KeyManager.getInstance().performAction(
                KeyTools.createKey(key),
                object : CommonCallbacks.CompletionCallbackWithParam<EmptyMsg> {
                    override fun onSuccess(t: EmptyMsg?) {
                        if (cont.isActive) cont.resume(Unit)
                    }

                    override fun onFailure(error: IDJIError) {
                        if (cont.isActive) cont.resumeWithException(RuntimeException(error.description()))
                    }
                }
            )
        }

    private fun logStatus(message: String) {
        tvMissionStatus.append("\n$message")
    }

    override fun onDestroy() {
        super.onDestroy()
        missionJob?.cancel()
        scope.cancel()
        KeyManager.getInstance().cancelListen(this)
    }
}
