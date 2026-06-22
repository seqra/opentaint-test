package com.example.approximations;

import org.opentaint.ir.approximation.annotation.Approximate;
import org.opentaint.jvm.dataflow.approximations.ArgumentTypeContext;
import org.opentaint.jvm.dataflow.approximations.OpentaintNdUtil;

@Approximate(java.io.File.class)
public class FileListFilesApproximation {
    public java.io.File[] listFiles() {
        java.io.File self = (java.io.File) (Object) this;
        if (OpentaintNdUtil.nextBool()) return null;
        return new java.io.File[] { self };
    }

    public java.io.File[] listFiles(@ArgumentTypeContext java.io.FileFilter filter) {
        java.io.File self = (java.io.File) (Object) this;
        if (filter != null && OpentaintNdUtil.nextBool()) {
            filter.accept(self);
        }
        if (OpentaintNdUtil.nextBool()) return null;
        return new java.io.File[] { self };
    }

    public java.io.File[] listFiles(@ArgumentTypeContext java.io.FilenameFilter filter) {
        java.io.File self = (java.io.File) (Object) this;
        if (filter != null && OpentaintNdUtil.nextBool()) {
            filter.accept(self, self.getName());
        }
        if (OpentaintNdUtil.nextBool()) return null;
        return new java.io.File[] { self };
    }
}
