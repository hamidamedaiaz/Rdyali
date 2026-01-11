package com.softpath.riverpath.opengl;

import com.softpath.riverpath.fileparser.MeshResolution;
import com.softpath.riverpath.util.DomainProperties;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDateTime;

import static com.softpath.riverpath.fileparser.MeshFileParser.parseFile2TriangleMesh;

class OpenGLViewerTest {

    @Test
    void show() {
        DomainProperties.getInstance().setDimension(3);
        System.out.println(LocalDateTime.now() + " : Start loading .t file");
        MeshResolution meshResolution = parseFile2TriangleMesh(new File("C:\\Users\\t372639\\.riverpath\\geometrycombined\\import_20251221231232\\geometrycombined.t"));
        System.out.println(LocalDateTime.now() + " : End loading .t file");
        OpenGLViewer.show(meshResolution.getTriangleMesh());
        System.out.println(LocalDateTime.now() + " : End openGL display");
        while (true) {

        }

    }
}