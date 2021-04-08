package com.atiqur.bluetoothjoystick.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class Joystick {
    private int BOUNDARIES = 100;
    private int MAX = 1024;
    private int OFFSET = 0;
    public int X_DIRECTION = 2;
    public int Y_DIRECTION = 1;
    private float distance = 0.0f;
    private final DrawCanvas draw;
    private boolean jDirection = false;
    private final ViewGroup mLayout;
    private int min_distance = 0;
    private boolean out = false;
    public Paint paint;
    private ViewGroup.LayoutParams params;
    private int position_x = 0;
    private int position_y = 0;
    public Bitmap stick;
    public int stick_height;
    public int stick_width;
    private boolean touch_state = false;

    public Joystick(Context context, ViewGroup layout, int stick_res_id) {
        this.stick = BitmapFactory.decodeResource(context.getResources(), stick_res_id);
        this.stick_width = this.stick.getWidth();
        this.stick_height = this.stick.getHeight();
        this.draw = new DrawCanvas(context);
        this.paint = new Paint();
        this.mLayout = layout;
        this.params = mLayout.getLayoutParams();
    }

    public void drawStick(MotionEvent arg1) {
        int choose;
        if (jDirection) {
            this.position_x = (int) (((arg1.getX() - ((float) (this.params.width / 2))) / ((float) ((this.params.width / 2) - this.OFFSET))) * ((float) this.MAX));
            this.position_y = (int) (arg1.getY() - ((float) (this.params.height / 2)));
        } else {
            this.position_x = (int) (arg1.getX() - ((float) (this.params.width / 2)));
            this.position_y = (int) (((arg1.getY() - ((float) (this.params.height / 2))) / ((float) ((this.params.height / 2) - this.OFFSET))) * ((float) this.MAX));
        }
        this.distance = (float) Math.sqrt(Math.pow((double) this.position_x, 2.0d) + Math.pow((double) this.position_y, 2.0d));
        if (arg1.getAction() == MotionEvent.ACTION_DOWN) {
            if (this.distance <= ((float) this.MAX)) {
                this.out = false;
                if (this.jDirection) {
                    this.draw.position(arg1.getX(), (float) (this.params.height / 2));
                } else {
                    this.draw.position((float) (this.params.width / 2), arg1.getY());
                }
                draw();
                this.touch_state = true;
            }
        } else if (arg1.getAction() == MotionEvent.ACTION_MOVE && this.touch_state) {
            if (this.jDirection) {
                choose = this.params.height / 2;
            } else {
                choose = this.params.width / 2;
            }
            if (this.distance <= ((float) (choose - this.OFFSET))) {
                this.out = false;
                if (this.jDirection) {
                    this.draw.position(arg1.getX(), (float) (this.params.height / 2));
                } else {
                    this.draw.position((float) (this.params.width / 2), arg1.getY());
                }
                draw();
            } else if (this.distance > ((float) (choose - this.OFFSET))) {
                if (this.jDirection) {
                    drawX(arg1);
                } else {
                    drawY(arg1);
                }
                draw();
            } else {
                this.mLayout.removeView(this.draw);
            }
        } else if (arg1.getAction() == 1) {
            this.draw.position((float) (this.params.width / 2), (float) (this.params.height / 2));
            draw();
            this.touch_state = false;
        }
    }

    private void drawX(MotionEvent arg1) {
        float x = arg1.getX();
        if (x > ((float) (this.params.width - this.OFFSET))) {
            x = (float) (this.params.width - this.OFFSET);
        } else if (x < ((float) this.OFFSET)) {
            x = (float) this.OFFSET;
        }
        if (arg1.getY() - ((float) (this.params.height / 2)) > ((float) this.BOUNDARIES)) {
            this.out = true;
            x = (float) (this.params.width / 2);
            this.position_x = (int) x;
        } else if (((float) (this.params.height / 2)) - arg1.getY() > ((float) this.BOUNDARIES)) {
            this.out = true;
            x = (float) (this.params.width / 2);
            this.position_x = (int) x;
        } else {
            this.out = false;
        }
        this.draw.position(x, (float) (this.params.height / 2));
    }

    private void drawY(MotionEvent arg1) {
        float y = arg1.getY();
        if (y > ((float) (this.params.height - this.OFFSET))) {
            y = (float) (this.params.height - this.OFFSET);
        } else if (y < ((float) this.OFFSET)) {
            y = (float) this.OFFSET;
        }
        if (arg1.getX() - ((float) (this.params.width / 2)) > ((float) this.BOUNDARIES)) {
            this.out = true;
            y = (float) (this.params.height / 2);
            this.position_y = (int) y;
        } else if (((float) (this.params.width / 2)) - arg1.getX() > ((float) this.BOUNDARIES)) {
            this.out = true;
            y = (float) (this.params.height / 2);
            this.position_y = (int) y;
        } else {
            this.out = false;
        }
        this.draw.position((float) (this.params.width / 2), y);
    }

    public int getX() {
        int position = (this.position_x + this.MAX) / 2;
        if (this.distance <= ((float) this.min_distance) || !this.touch_state) {
            return this.MAX / 2;
        }
        if (this.out) {
            return this.MAX / 2;
        }
        if (position > this.MAX) {
            position = this.MAX;
        } else if (position < 0) {
            position = 0;
        }
        return position;
    }

    public int getY() {
        int position = (this.MAX - this.position_y) / 2;
        if (this.distance <= ((float) this.min_distance) || !this.touch_state) {
            return this.MAX / 2;
        }
        if (this.out) {
            return this.MAX / 2;
        }
        if (position > this.MAX) {
            position = this.MAX;
        } else if (position < 0) {
            position = 0;
        }
        return position;
    }

    public void setMinimumDistance(int minDistance) {
        this.min_distance = minDistance;
    }

    public void setOffset(int offset) {
        this.OFFSET = offset;
    }

    public void setStickAlpha(int alpha) {
        this.paint.setAlpha(alpha);
    }

    public void setLayoutAlpha(int alpha) {
        this.mLayout.getBackground().setAlpha(alpha);
    }

    public void setStickSize(int width, int height) {
        this.stick = Bitmap.createScaledBitmap(this.stick, width, height, false);
        this.stick_width = this.stick.getWidth();
        this.stick_height = this.stick.getHeight();
    }

    public void setLayoutSize(int width, int height) {
        this.params.width = width;
        this.params.height = height;
    }

    public void drawZeroPos() {
        this.draw.position((float) (this.params.width / 2), (float) (this.params.height / 2));
        draw();
    }

    public void setMax(int value) {
        this.MAX = value;
    }

    public void setBoundaries(int value) {
        this.BOUNDARIES = value;
    }

    public void setDirection(int direction) {
        if (direction == this.Y_DIRECTION) {
            this.jDirection = false;
        } else if (direction == this.X_DIRECTION) {
            this.jDirection = true;
        }
    }

    private void draw() {
        try {
            this.mLayout.removeView(this.draw);
        } catch (Exception e) {
        }
        this.mLayout.addView(this.draw);
    }

    private class DrawCanvas extends View {

        /* renamed from: x */
        float f21x;

        /* renamed from: y */
        float f22y;

        private DrawCanvas(Context mContext) {
            super(mContext);
        }

        public void onDraw(Canvas canvas) {
            canvas.drawBitmap(Joystick.this.stick, this.f21x, this.f22y, Joystick.this.paint);
        }

        /* access modifiers changed from: private */
        public void position(float pos_x, float pos_y) {
            this.f21x = pos_x - ((float) (Joystick.this.stick_width / 2));
            this.f22y = pos_y - ((float) (Joystick.this.stick_height / 2));
        }
    }
}
