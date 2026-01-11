package com.softpath.riverpath.meshconverter;

/**
 * Interface for mesh converters (GMSH to MTC format)
 */
public interface MeshConverter {

    /**
     * Convert mesh file to MTC format
     *
     * @param inputPath  path to input .msh file
     * @param outputPath path to output .t file
     */
    void convert(String inputPath, String outputPath);

    /**
     * Get the dimension this converter handles
     */
    int getDimension();
}
