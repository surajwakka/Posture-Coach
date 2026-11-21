package com.posturecoach;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.Joints;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.inference.Predictor;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.List;

/** Decodes JPEGs with OpenCV (via DJL) and runs SimplePose (COCO 17 keypoints). */
@Service
public class PoseService {
    private static final Logger log = LoggerFactory.getLogger(PoseService.class);
    private ZooModel<Image, Joints> model;
    private Predictor<Image, Joints> predictor;

    @PostConstruct
    void init() throws Exception {
        Criteria<Image, Joints> criteria = Criteria.builder()
                .setTypes(Image.class, Joints.class)
                .optModelUrls("djl://ai.djl.mxnet/simple_pose")
                .optFilter("backbone", "resnet18")
                .optFilter("flavor", "v1b")
                .optFilter("dataset", "imagenet")
                .build();
        model = criteria.loadModel();
        predictor = model.newPredictor();
        log.info("Pose model loaded");
    }

    /** Predictor is not thread-safe, so serialize inference. */
    public synchronized PostureMetrics analyze(byte[] jpeg) throws Exception {
        Image img = ImageFactory.getInstance().fromInputStream(new ByteArrayInputStream(jpeg));
        Joints joints = predictor.predict(img);
        List<Joints.Joint> j = joints.getJoints();
        return PostureMetrics.from(j, img.getWidth(), img.getHeight());
    }

    @PreDestroy
    void close() {
        if (predictor != null) predictor.close();
        if (model != null) model.close();
    }
}
