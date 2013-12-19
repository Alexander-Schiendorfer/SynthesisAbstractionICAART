package de.uniaugsburg.isse.csp.util;

import java.io.File;

import javax.swing.filechooser.FileFilter;

/**
 * A simple {@link FileFilter} to enable selection of OPL model files
 * (extension: .mod).
 * 
 * @author Jan-Philipp Steghöfer
 */
public class OplModelFileFilter extends FileFilter {

    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }

        String name = f.getName().toLowerCase();

        if (name.endsWith("mod")) {
            return true;
        }

        return false;
    }

    @Override
    public String getDescription() {
        return "OPL model files (*.mod)";
    }

}