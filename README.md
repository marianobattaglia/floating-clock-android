# FloatingClock (Android TV)

FloatingClock is a lightweight Android TV app that shows the current time and can display a floating clock overlay on top of other apps. It also includes an alarm that can ring while you are watching videos or using other apps.

## Screenshots and Media

- App View
<img width="3840" height="2160" alt="app-view" src="https://github.com/user-attachments/assets/4592a17b-66f1-485e-b694-0754af03fe64" />

- Clock Position Setup Modal View
<img width="3840" height="2160" alt="app-view" src="https://github.com/user-attachments/assets/79763380-dc6c-4cd8-97da-b9c9474079bf" />

- Alarm Setup Modal View
<img width="3840" height="2160" alt="app-alarm-modal" src="https://github.com/user-attachments/assets/830e3ef5-1a0f-45a4-a1af-6db8ae8b7a28" />

- Floating Clock Overlay
<img width="3840" height="2160" alt="overlay-on" src="https://github.com/user-attachments/assets/7f12bc8e-c20f-4d39-a776-35e3084bd752" />

- Floating Clock Overlay (Ringing)
<img width="3840" height="2160" alt="overlay-alarm" src="https://github.com/user-attachments/assets/0f818c36-a19c-43fc-aebe-95eddea8ccf2" />

## Demo

https://github.com/user-attachments/assets/2404c8c6-d9b0-43e4-8aec-4f57f02c841b

## Features

- Live clock on the main screen.
- Floating overlay clock that stays visible on top of other apps.
- Alarm with HH:MM configuration in 24-hour format.
- Alarm sound plays in the background and the overlay clock blinks red while it is active.
- Alarm can be dismissed by entering the app and selecting the Alarm row.

## How It Works

1) The overlay switch starts a foreground service that draws a small clock window using `SYSTEM_ALERT_WINDOW`.
2) The alarm schedule uses `AlarmManager` to trigger at the selected time.
3) When the alarm fires, a foreground service plays an alarm sound and broadcasts alarm state updates.
4) The floating clock listens for those updates and blinks red while the alarm is active.

## Permissions Used

- `SYSTEM_ALERT_WINDOW`: Required to draw the floating overlay clock.
- `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_SPECIAL_USE`: Required for the overlay service.
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: Required for the alarm sound service.
- `POST_NOTIFICATIONS`: Required on Android 13+ for foreground service notifications.
- `SCHEDULE_EXACT_ALARM`: Used when available to schedule exact alarms.

## Usage

1) Open the app and toggle **Show Floating Clock** to enable the overlay.
2) Go to **Alarm** row to set the alarm time.
3) Press **ACTIVATE** to schedule the alarm or **CANCEL** to disable it.
4) When the alarm rings, the overlay clock blinks red and an alarm sound plays.
5) Open the app and select the **Alarm** row to stop the alarm immediately.

## Notes

- On some Android TV devices or emulators, exact alarms may not be allowed. In that case, the app falls back to a best-effort schedule.
- If the emulator has no default alarm sound, the app falls back to a simple beep tone.
