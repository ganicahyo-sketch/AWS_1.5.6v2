# STASIUN CUACA v1.5.7 — USB Soil Meter 8-in-1 alignment

User confirmed the hardware is the USB Soil Meter 8-in-1 shown in `Soil_Detector_1.0.2-1(1).apk`, not a CWT-7in1 USB sensor.

## APK inspection
The supplied APK contains CH34x USB serial driver strings and adapter IDs:
- `1A86:5512`
- `1A86:5523`
- `1A86:7523`
- `1A86:E010`

The v1.5.7 `UsbSoilMeter` adapter filter already includes these same four IDs and uses Modbus RTU function 0x04 first, with 0x03 fallback. It attempts eight registers and falls back to two four-register requests if the meter firmware limits request length. This behavior is retained.

## Changes
- Corrected the source label from `CWT-7in1 USB` to `USB Soil Meter 8-in-1 (CH34x)`.
- Corrected Catatan Lapangan heading and helper/status text so the physical 8-in-1 sensor is not mislabeled as CWT-7in1.
- Preserved CWT-7in1 RS485 and manual input choices.
- No other application features intentionally changed.

## Verification
The supplied APK's DEX strings were inspected for CH34x driver and USB IDs. The ZIP integrity is checked. Hardware-level register mapping and a full Android build still require testing on the actual meter and GitHub Actions. Sensor register scaling remains model/firmware-dependent; compare the eight displayed values with the meter/manual before using them for agronomic decisions.
