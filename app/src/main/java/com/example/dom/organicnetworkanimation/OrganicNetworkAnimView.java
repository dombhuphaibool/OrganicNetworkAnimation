package com.example.dom.organicnetworkanimation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by dom on 11/3/15.
 */
public class OrganicNetworkAnimView extends View {

    private static final float SCROLL_SPEED_PX_PER_SEC = 50.0f;

    private static final float NETWORK_STROKE_WIDTH = 8.0f;
    private static final float NETOWRK_NODE_RADIUS = 25.0f;
    private static final float FLOATER_NODE_RADIUS = 35.0f;

    private static final float MOTION_BOUNDS_DX = 25.0f; // Px, consider using Dp instead
    private static final float MOTION_BOUNDS_DY = 25.0f; // Px, consider using Dp instead
    private static final float FLOAT_MOTION_BOUNDS_DX = 50.0f; // Px, consider using Dp instead
    private static final float FLOAT_MOTION_BOUNDS_DY = 25.0f; // Px, consider using Dp instead

    private static final int PROBABILITY_CONNECT_FIRST_PATH = 89;
    private static final int PROBABILITY_CONNECT_SECOND_PATH = PROBABILITY_CONNECT_FIRST_PATH - 14;
    private static final int PROBABILITY_CONNECT_SECOND_END_PATH = PROBABILITY_CONNECT_FIRST_PATH - 12;

    private static final int PROBABILITY_GENERATE_LARGE_FLOATERS = 65;

    private static final int NUM_LEFT_NODES = 12;
    private static final int NUM_RIGHT_NODES = 12;
    private static final int NUM_FLOATERS = 10;

    private static class Node {
        private final PointF mOrigPos;
        private final PointF mPos;
        private final RectF mMotionBounds;
        private final PointF mMotionVector;
        private float mLastTimeSec;
        private float mRadius;
        private int mNumRings;
        private Paint mPaint;

        public Node(Paint paint, float x, float y, float radius) {
            this(paint, x, y, radius, 1);
        }

        public Node(Paint paint, float x, float y, float radius, int numRings) {
            this(paint, x, y, radius, numRings, MOTION_BOUNDS_DX, MOTION_BOUNDS_DY);
        }

        public Node(Paint paint, float x, float y, float radius, int numRings, float motionBoundsDx, float motionBoundsDy) {
            mPaint = paint;
            mOrigPos = new PointF(x, y);
            mPos = new PointF(x, y);
            mMotionBounds = new RectF(x - motionBoundsDx, y - motionBoundsDy, x + motionBoundsDx, y + motionBoundsDy);
            mMotionVector = new PointF();
            mLastTimeSec = 0.0f;
            mRadius = radius;
            mNumRings = numRings;
        }

        public Paint getPaint() {
            return mPaint;
        }

        public float getRadius() {
            return mRadius;
        }

        public int getNumRings() {
            return mNumRings;
        }

        public float adjustX(float dx) {
            mPos.x = mOrigPos.x + dx;
            return mPos.x;
        }

        public float adjustY(float dy) {
            mPos.y = mOrigPos.y + dy;
            return mPos.y;
        }

        public float adjustY(float dy, float minThreshold, float maxThreshold) {
            float newYPos = mOrigPos.y + dy;
            if (newYPos < minThreshold) {           // for negative dy
                mPos.y = maxThreshold + (mOrigPos.y - minThreshold) + dy;
            } else if (newYPos > maxThreshold) {    // for positive dy
                mPos.y = minThreshold + dy - (maxThreshold - mOrigPos.y);
            } else {
                mPos.y = newYPos;
            }
            return mPos.y;
        }

        public float updateX() {
            mPos.x = mOrigPos.x;
            return mPos.x;
        }

        public void initMotionVector(Random rand) {
            float width = mMotionBounds.width() * 0.8f;
            float height = mMotionBounds.height() * 0.4f;
            mMotionVector.x = (width * rand.nextFloat()) - (width * 0.5f);
            mMotionVector.y = (height * rand.nextFloat()) - (height * 0.5f);
        }

        public void adjustMotion(float timeSec, Random rand) {
            float deltaSec = timeSec - mLastTimeSec;
            mLastTimeSec = timeSec;
            float newX = mOrigPos.x + (deltaSec * mMotionVector.x);
            float newY = mOrigPos.y + (deltaSec * mMotionVector.y);
            if (!mMotionBounds.contains(newX, newY)) {
                initMotionVector(rand);
            } else {
                mOrigPos.set(newX, newY);
            }
        }

        public float getX() {
            return mPos.x;
        }

        public float getY() {
            return mPos.y;
        }
    }

    private static class NodePath {
        private final List<Node> mNodes;
        private final int mIdx1;
        private final int mIdx2;

        NodePath(List<Node> nodes, int idx1, int idx2) {
            mNodes = nodes;
            mIdx1 = idx1;
            mIdx2 = idx2;
        }

        Node getNode1() {
            return mNodes.get(mIdx1);
        }

        Node getNode2() {
            return mNodes.get(mIdx2);
        }
    }

    private int mNumLeftNodes;
    private int mNumRightNodes;
    private int mNumFloaters;

    private List<Node> mLeftNodes;
    private List<Node> mRightNodes;
    private List<NodePath> mLeftPaths;
    private List<NodePath> mRightPaths;
    private List<Node> mFloatingNodes;

    private Paint mNodePaint;
    private Paint mNetworkPaint;
    private Paint mFloaterPaint;

    private float mWidth;
    private float mHeight;

    private Random mRand;
    private long mBaseTime;
    private long mAnimTime;

    public OrganicNetworkAnimView(Context context) {
        this(context, null, 0);
    }

    public OrganicNetworkAnimView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OrganicNetworkAnimView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);

        if (attrs != null) {

        }
    }

    private void init(Context context) {
        mRand = new Random();
        mBaseTime = System.currentTimeMillis();
        mAnimTime = mBaseTime;

        mNumLeftNodes = NUM_LEFT_NODES;
        mNumRightNodes = NUM_RIGHT_NODES;
        mNumFloaters = NUM_FLOATERS;

        mLeftNodes = new ArrayList<>();
        mRightNodes = new ArrayList<>();
        mLeftPaths = new ArrayList<>();
        mRightPaths = new ArrayList<>();
        mFloatingNodes = new ArrayList<>();

        mNodePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mNodePaint.setStyle(Paint.Style.FILL);
        mNodePaint.setColor(getResources().getColor(R.color.node_white));

        mNetworkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mNetworkPaint.setStrokeWidth(NETWORK_STROKE_WIDTH);
        mNetworkPaint.setColor(getResources().getColor(R.color.network_white));

        mFloaterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mFloaterPaint.setStyle(Paint.Style.FILL);
        mFloaterPaint.setColor(getResources().getColor(R.color.float_white));

        setBackgroundColor(getResources().getColor(R.color.dark_teal));
    }

    private void generateNodes(List<Node> list, int numNodes, float xMin, float xMax, float xRange) {
        float x, y, offset;
        Node node;
        float ySegment = mHeight * 2.0f / numNodes;
        float yBase = -mHeight * 0.5f;
        for (int i=0; i<numNodes; ++i) {
            offset = mRand.nextFloat() * xRange;
            x = (i % 2 == 0) ? xMin + offset : xMax - offset;
            y = yBase + (ySegment * i); // @TODO: Jitter the y offset
            node = new Node(mNodePaint, x, y, NETOWRK_NODE_RADIUS);
            node.initMotionVector(mRand);
            list.add(node);
        }
    }

    private void adjustNodes(List<Node> nodes, long animTime) {
        float adjustedAmount;
        float animTimeSec = animTime * 0.001f;
        adjustedAmount = ((animTimeSec * SCROLL_SPEED_PX_PER_SEC) % (mHeight * 2.0f));
        float minThreshold = -mHeight * 0.5f;
        float maxThreshold = mHeight * 1.5f;
        for (Node node : nodes) {
            node.adjustY(-adjustedAmount, minThreshold, maxThreshold);
            node.adjustMotion(animTimeSec, mRand);
            node.updateX();
        }
    }

    private void generateFloatingNodes(List<Node> list, int numNodes, float xMin, float xMax, float xRange) {
        float x, y, offset;
        int numRings;
        Node node;
        float yRange = mHeight * 2.0f;
        float yOffset = -mHeight * 0.5f;
        for (int i=0; i<numNodes; ++i) {
            numRings = mRand.nextInt(100) > PROBABILITY_GENERATE_LARGE_FLOATERS ? 3 : 1;
            offset = mRand.nextFloat() * xRange;
            x = (i % 2 == 0) ? xMin + offset : xMax - offset;
            y = (mRand.nextFloat() * yRange) + yOffset;
            node = new Node(mFloaterPaint, x, y, FLOATER_NODE_RADIUS, numRings, FLOAT_MOTION_BOUNDS_DX, FLOAT_MOTION_BOUNDS_DY);
            node.initMotionVector(mRand);
            list.add(node);
        }
    }

    private boolean addPath(List<NodePath> paths, NodePath path, int percentage) {
        boolean added = false;
        if (mRand.nextInt(100) < percentage) {
            paths.add(path);
            added = true;
        }
        return added;
    }

    private void generatePaths(List<NodePath> paths, List<Node> nodes) {
        int firstPathPercentage = PROBABILITY_CONNECT_FIRST_PATH;
        int secondPathPercentage = PROBABILITY_CONNECT_SECOND_PATH;
        int secondEndPathPercentage = PROBABILITY_CONNECT_SECOND_END_PATH;

        int numNodes = nodes.size();
        for (int i=0; i<numNodes-2; ++i) {
            addPath(paths, new NodePath(nodes, i, i + 1), firstPathPercentage);
            addPath(paths, new NodePath(nodes, i, i+2), secondPathPercentage);
        }

        if (numNodes > 2) {
            addPath(paths, new NodePath(nodes, numNodes - 2, numNodes - 1), firstPathPercentage);
            addPath(paths, new NodePath(nodes, numNodes - 2, 0), secondEndPathPercentage);
        }

        if (numNodes > 1) {
            addPath(paths, new NodePath(nodes, numNodes - 1, 0), firstPathPercentage);
            addPath(paths, new NodePath(nodes, numNodes - 1, 1), secondEndPathPercentage);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // Account for padding
        float paddingLeft = (float) getPaddingLeft();
        float paddingTop = (float) getPaddingTop();

        float xPadding = paddingLeft + (float) getPaddingRight();
        float yPadding = paddingTop + (float) getPaddingBottom();

        mWidth = (float) w - xPadding;
        mHeight = (float) h - yPadding;

        generateNodes(mLeftNodes, mNumLeftNodes, -mWidth * 0.4f, mWidth * 0.3f, mWidth * 0.25f);
        generateNodes(mRightNodes, mNumRightNodes, mWidth * 0.7f, mWidth * 1.4f, mWidth * 0.25f);
        generatePaths(mLeftPaths, mLeftNodes);
        generatePaths(mRightPaths, mRightNodes);
        generateFloatingNodes(mFloatingNodes, mNumFloaters, mWidth * 0.1f, mWidth * 0.9f, mWidth * 0.2f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawPaths(canvas, mLeftPaths);
        drawPaths(canvas, mRightPaths);
        drawNodes(canvas, mLeftNodes);
        drawNodes(canvas, mRightNodes);
        drawNodes(canvas, mFloatingNodes);

        mAnimTime = (System.currentTimeMillis() - mBaseTime);
        adjustNodes(mLeftNodes, mAnimTime);
        adjustNodes(mRightNodes, mAnimTime);
        adjustNodes(mFloatingNodes, mAnimTime);

        postInvalidate();
    }

    private void drawNodes(Canvas canvas, List<Node> nodes) {
        float radius;
        int numRings;
        for (Node node : nodes) {
            radius = node.getRadius();
            numRings = node.getNumRings();
            for (int i=1; i<=numRings; ++i) {
                canvas.drawCircle(node.getX(), node.getY(), radius, node.getPaint());
                radius += 10.0f * i;
            }
        }
    }

    private void drawPaths(Canvas canvas, List<NodePath> paths) {
        for (NodePath path : paths) {
            Node node1 = path.getNode1();
            Node node2 = path.getNode2();
            if (node2.getY() - node1.getY() > 0) {
                canvas.drawLine(node1.getX(), node1.getY(), node2.getX(), node2.getY(), mNetworkPaint);
            }
        }
    }
}
