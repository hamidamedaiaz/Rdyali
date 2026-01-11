package com.softpath.riverpath.fileparser;

import com.softpath.riverpath.util.DomainProperties;
import org.junit.jupiter.api.Test;

import java.io.File;

import static com.softpath.riverpath.fileparser.MeshFileParser.parseFile2TriangleMesh;

class CFDTriangleMeshTest {

    @Test
    public void test() {
        DomainProperties.getInstance().setDimension(3);
        MeshResolution mesResolution = parseFile2TriangleMesh(new File("FILE_PATH_HERE"));
        /*System.out.println(mesResolution.getTriangleMesh().getTriangles().size());
        System.out.println(mesResolution.getReducedMesh().getTriangles().size());
        System.out.println(mesResolution.getTriangleSurface().getTriangles().size());*/
    }

    @Test
    public void test2D() {
        MeshResolution mesResolution = parseFile2TriangleMesh(new File("FILE_PATH_HERE"));
        /*System.out.println(mesResolution.getTriangleMesh().getTriangles().size());
        System.out.println(mesResolution.getReducedMesh().getTriangles().size());
        System.out.println(mesResolution.getTriangleSurface().getTriangles().size());*/
    }

}