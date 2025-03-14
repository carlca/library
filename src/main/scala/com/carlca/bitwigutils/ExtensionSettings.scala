package com.carlca.bitwiglibrary

import scala.math._
import com.bitwig.extension.controller.api.*
import com.bitwig.extension.callback.DoubleValueChangedCallback
// import com.carlca.logger.Log

object ExtensionSettings:
  enum SettingsCapability derives CanEqual:
    case `Solo Behaviour`, `Track Mapping Behaviour`, `Third Row Behaviour`, `Fader dB Range`, `Master dB Range`
  enum PanSendMode derives CanEqual:
    case `FX Send`, `Pan`
  enum TrackMode derives CanEqual:
    case `One to One`, `No Groups`, `Groups Only`, `Tagged "<>" Only`
  // Set of settings that can be changed in the preferences - initiallised with no options
  var settingsCapabilities: Set[SettingsCapability] = Set.empty
  // add Solo Behaviour to the set of settings that can be changed in the preferences
  // settingsCapabilities += SettingsCapability.`Solo Behaviour`

  var exclusiveSolo: Boolean = false
  var panSendMode: PanSendMode = PanSendMode.`FX Send`
  var trackMode:  TrackMode = TrackMode.`One to One`

  private val MIN = "min"
  private val MAX = "max"
  private val MASTER = 0
  private val NUM_TRACKS = 8
  private val MIN_DB = -160.0
  private val MAX_DB = 6.0
  private val DB_INCREMENT = 0.1

  // min/max dB values for each track
  private val trackMinDb = Array.fill[Double](NUM_TRACKS + 1)(Double.NegativeInfinity)  // +1 for Master
  private val trackMaxDb = Array.fill[Double](NUM_TRACKS + 1)(6.0)                      // +1 for Master

  def init(host: ControllerHost) =
    initPreferences(host)

  def initPreferences(host: ControllerHost): Unit =
    val prefs = host.getPreferences

    if settingsCapabilities.contains(SettingsCapability.`Solo Behaviour`) then
      val soloSetting = prefs.getBooleanSetting("Exclusive Solo", "Solo Behaviour", false)
      soloSetting.addValueObserver((value) => ExtensionSettings.exclusiveSolo = value)

    if settingsCapabilities.contains(SettingsCapability.`Third Row Behaviour`) then
      val values = PanSendMode.values.map(_.toString).toArray
      val panSetting = prefs.getEnumSetting("Send/Pan Mode", "Third Row Behaviour", values, PanSendMode.`FX Send`.toString())
      panSetting.addValueObserver((value) => ExtensionSettings.panSendMode = PanSendMode.valueOf(value))

    if settingsCapabilities.contains(SettingsCapability.`Track Mapping Behaviour`) then
      val trackModes = TrackMode.values.map(_.toString).toArray
      val trackSetting = prefs.getEnumSetting("Track Mode", "Track Mapping Behaviour", trackModes, TrackMode.`One to One`.toString())
      trackSetting.addValueObserver((value) => ExtensionSettings.trackMode = TrackMode.valueOf(value))

    def createTrackDbSetting(minMax: String, trackNumber: Int): SettableRangedValue =
      val settingName = if trackNumber == 0 then "Master dB" else s"Fader $trackNumber dB"
      prefs.getNumberSetting(minMax, settingName, MIN_DB, MAX_DB, 0.1, null,
        if minMax == MIN then MIN_DB else MAX_DB
      )

    def addTrackDbObserver(setting: SettableRangedValue, minMax: String, trackNumber: Int): Unit =
      setting.addRawValueObserver(new DoubleValueChangedCallback:
        override def valueChanged(newValue: Double): Unit =
          if minMax == MIN then
              if newValue > trackMaxDb(trackNumber) then
                  createTrackDbSetting(MAX, trackNumber).set(newValue)
                  trackMaxDb(trackNumber) = newValue
                  trackMinDb(trackNumber) = newValue
              else
                  trackMinDb(trackNumber) = newValue
          else
              if newValue < trackMinDb(trackNumber) then
                  createTrackDbSetting(MIN, trackNumber).set(newValue)
                  trackMaxDb(trackNumber) = newValue
                  trackMinDb(trackNumber) = newValue
              else
                trackMaxDb(trackNumber) = newValue
      )

    if settingsCapabilities.contains(SettingsCapability.`Fader dB Range`) then
      // Create settings and observers for each track
      for trackNumber <- 1 to NUM_TRACKS do
        val minSetting = createTrackDbSetting(MIN, trackNumber)
        trackMinDb(trackNumber) = minSetting.getRaw // Initial values
        addTrackDbObserver(minSetting, MIN, trackNumber)

        val maxSetting = createTrackDbSetting(MAX, trackNumber)
        trackMaxDb(trackNumber) = maxSetting.getRaw // Initial values
        addTrackDbObserver(maxSetting, MAX, trackNumber)

    if settingsCapabilities.contains(SettingsCapability.`Master dB Range`) then
      // Master Track
      val masterMinSetting = createTrackDbSetting(MIN, MASTER)
      trackMinDb(MASTER) = masterMinSetting.getRaw
      addTrackDbObserver(masterMinSetting, MIN, MASTER)

      val masterMaxSetting = createTrackDbSetting(MAX, MASTER)
      trackMaxDb(MASTER) = masterMaxSetting.getRaw
      addTrackDbObserver(masterMaxSetting, MAX, MASTER)

  def getVolumeRange(track: Int): (Double, Double) =
    if settingsCapabilities.contains(SettingsCapability.`Fader dB Range`) then
      (dbToVolume(trackMinDb(track + 1)), dbToVolume(trackMaxDb(track + 1)))
    else
      (dbToVolume(MIN_DB), dbToVolume(MAX_DB))

  def getMasterVolumeRange: (Double, Double) =
    if settingsCapabilities.contains(SettingsCapability.`Master dB Range`) then
      (dbToVolume(trackMinDb(MASTER)), dbToVolume(trackMaxDb(MASTER)))
    else
      (dbToVolume(MIN_DB), dbToVolume(MAX_DB))

  private val dbToVolumeLookupTable: Array[Double] =
    val steps = ((MAX_DB - MIN_DB) * 10).toInt
    val table = new Array[Double](steps + 1)
    for i <- 0 to steps do
      val db = MIN_DB + i * DB_INCREMENT
      table(i) = pow(E, ((db + 120.2) / 26.056))
    table

  private def dbToVolume(db: Double): Double =
    if db < MIN_DB then
      dbToVolumeLookupTable(0)
    else if db > MAX_DB then
      dbToVolumeLookupTable(dbToVolumeLookupTable.length - 1)
    else
      val index = math.round(((db - MIN_DB) / DB_INCREMENT)).toInt
      dbToVolumeLookupTable(index)

  // SoloBehaviour
  // TrackMappingBehaviour
  // ThirdRowBehaviour
  // MasterDbRnage
  // TrackDbRange
