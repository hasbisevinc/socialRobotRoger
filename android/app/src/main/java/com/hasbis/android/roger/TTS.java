package com.hasbis.android.roger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Created by hasbis on 19.02.2018.
 */
public class TTS {
    public static final String TAG = HotWord.class.getName();

    private static TTS instance = null;
    private Activity activity = null;
    TextToSpeech tts;
    boolean freeze = false;

    private TTS(final Activity activity) {
        setContext(activity);
        tts = new TextToSpeech(activity, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.ENGLISH);
                }
            }
        });

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Roger.canvas.status = 1;
                    }
                });
            }

            @Override
            public void onDone(String s) {
                Log.d(TAG, "onDone: ");
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String lastAction = RegularWord.getInstance().lastAction;
                        if (lastAction.length() == 0) {
                            Roger.canvas.status = 0;
                        } else if (lastAction.equals("sad")) {
                            Roger.canvas.status = 3;
                        } else if (lastAction.equals("shock")) {
                            Roger.canvas.status = 2;
                        } else if (lastAction.equals("shock2")) {
                            Roger.canvas.status = 2;
                        } else if (lastAction.equals("angry")) {
                            Roger.canvas.status = 6;
                        } else if (lastAction.equals("think")) {
                            Roger.canvas.status = 4;
                        } else if (lastAction.equals("fun")) {
                            Roger.canvas.status = 5;
                        }
                    }
                });

                if (InteractionData.state == InteractionData.STATES.CHAT) {
                    STTEngine.getInstance().master = STTEngine.MASTER.EMTY;
                } else if (InteractionData.state == InteractionData.STATES.MOVEMENT) {
                    if (freeze) {
                        Log.d(TAG, "onDone: freeze");
                        return;
                    }
                    //STTEngine.getInstance().master = STTEngine.MASTER.EMTY;
                    STTEngine.getInstance().master = STTEngine.MASTER.SPEAKING;
                    if (InteractionData.movementIndex == 0) { // Let’s raise your left hand.
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        RobotApi.speak(activity, InteractionData.movementSessionWords[InteractionData.movementIndex]);
                        InteractionData.movementIndex++;
                        RobotApi.upLeftArm();
                        freeze = true;
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                RobotApi.downLeftArm();
                                freeze = false;
                                RobotApi.speak(activity, InteractionData.movementSessionWords[InteractionData.movementIndex]);
                            }
                        }, 3000);
                    } else if (InteractionData.movementIndex == 1) { // Let’s raise your right hand.
                        RobotApi.upRightArm();
                        InteractionData.movementIndex++;
                        freeze = true;
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                RobotApi.downRightArm();
                                freeze = false;
                                RobotApi.speak(activity, InteractionData.movementSessionWords[InteractionData.movementIndex]);
                            }
                        }, 3000);
                    }  else if (InteractionData.movementIndex == 2) { // Let’s raise your both hands.
                        RobotApi.upLeftArm();
                        RobotApi.upRightArm();
                        InteractionData.movementIndex++;
                        freeze = true;
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                freeze = false;
                                RobotApi.downRightArm();
                                RobotApi.downLeftArm();
                                RobotApi.speak(activity, InteractionData.movementSessionWords[InteractionData.movementIndex]);
                            }
                        }, 3000);
                    } else if (InteractionData.movementIndex == 3) { // Let’s move back.
                        RobotApi.moveBackward();
                        InteractionData.movementIndex++;
                        freeze = true;
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                freeze = false;
                                RobotApi.moveForward();
                                RobotApi.speak(activity, InteractionData.movementSessionWords[InteractionData.movementIndex]);
                            }
                        }, 3000);
                    } else if (InteractionData.movementIndex == 4) { // Let’s go ahead.
                        RobotApi.moveForward();
                        InteractionData.movementIndex++;
                        freeze = true;
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                RobotApi.moveBackward();
                                RobotApi.speak(activity, InteractionData.movementSessionWords[InteractionData.movementIndex]);
                                InteractionData.state = InteractionData.STATES.QUESTIONS;
                                freeze = false;
                            }
                        }, 3000);
                    } else {
                        InteractionData.state = InteractionData.STATES.QUESTIONS;
                    }
                } else if (InteractionData.state == InteractionData.STATES.QUESTIONS) {
                    if (InteractionData.questionIndex > 0) {
                        STTEngine.getInstance().master = STTEngine.MASTER.EMTY;
                    }

                    if (InteractionData.closeTheApp == true) {
                        AudioManager amanager=(AudioManager)activity.getSystemService(Context.AUDIO_SERVICE);

                        amanager.setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
                        amanager.setStreamMute(AudioManager.STREAM_ALARM, false);
                        amanager.setStreamMute(AudioManager.STREAM_MUSIC, false);
                        amanager.setStreamMute(AudioManager.STREAM_RING, false);
                        amanager.setStreamMute(AudioManager.STREAM_SYSTEM, false);

                        Intent LaunchIntent = activity.getPackageManager().getLaunchIntentForPackage("com.nana.Androidtest");
                        activity.startActivity(LaunchIntent);
                        System.exit(0);
                        //activity.finish();
                    }
                    if (InteractionData.questionIndex == 0){
                        RobotApi.speak(activity, InteractionData.questions[InteractionData.questionIndex]);
                        InteractionData.questionIndex++;
                    }
                }
            }

            @Override
            public void onError(String s) {
                Log.d(TAG, "onError: ");
                STTEngine.getInstance().master = STTEngine.MASTER.EMTY;
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Roger.canvas.status = 0;
                    }
                });
            }
        });
    }

    public void speek(String str) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"messageID");
        tts.speak(str, TextToSpeech.QUEUE_FLUSH, map);
    }

    public static TTS getInstance(Activity activity) {
        if (instance == null) {
            synchronized (TAG) {
                if (instance == null) {
                    instance = new TTS(activity);
                }
            }
        }
        return instance;
    }

    public void setContext(Activity activity) {
        this.activity = activity;
    }
}
