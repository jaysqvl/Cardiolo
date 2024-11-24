package com.example.myruns.services;

class WekaClassifier {

    public static double classify(Object[] i)
            throws Exception {

        double p = Double.NaN;
        p = WekaClassifier.N1709a0ea0(i);
        return p;
    }
    static double N1709a0ea0(Object []i) {
        double p = Double.NaN;
        if (i[0] == null) {
            p = 0;
        } else if (((Double) i[0]).doubleValue() <= 52.038272) {
            p = WekaClassifier.N221f0bc61(i);
        } else if (((Double) i[0]).doubleValue() > 52.038272) {
            p = WekaClassifier.N4b1b51c68(i);
        }
        return p;
    }
    static double N221f0bc61(Object []i) {
        double p = Double.NaN;
        if (i[0] == null) {
            p = 0;
        } else if (((Double) i[0]).doubleValue() <= 36.15209) {
            p = WekaClassifier.N19d08e292(i);
        } else if (((Double) i[0]).doubleValue() > 36.15209) {
            p = WekaClassifier.N71ce64be4(i);
        }
        return p;
    }
    static double N19d08e292(Object []i) {
        double p = Double.NaN;
        if (i[0] == null) {
            p = 0;
        } else if (((Double) i[0]).doubleValue() <= 6.128417) {
            p = WekaClassifier.N2b9a9fd73(i);
        } else if (((Double) i[0]).doubleValue() > 6.128417) {
            p = 0;
        }
        return p;
    }
    static double N2b9a9fd73(Object []i) {
        double p = Double.NaN;
        if (i[7] == null) {
            p = 2;
        } else if (((Double) i[7]).doubleValue() <= 0.21054) {
            p = 2;
        } else if (((Double) i[7]).doubleValue() > 0.21054) {
            p = 0;
        }
        return p;
    }
    static double N71ce64be4(Object []i) {
        double p = Double.NaN;
        if (i[15] == null) {
            p = 0;
        } else if (((Double) i[15]).doubleValue() <= 0.295962) {
            p = 0;
        } else if (((Double) i[15]).doubleValue() > 0.295962) {
            p = WekaClassifier.N2bd5107a5(i);
        }
        return p;
    }
    static double N2bd5107a5(Object []i) {
        double p = Double.NaN;
        if (i[10] == null) {
            p = 1;
        } else if (((Double) i[10]).doubleValue() <= 0.710214) {
            p = WekaClassifier.N366972ef6(i);
        } else if (((Double) i[10]).doubleValue() > 0.710214) {
            p = WekaClassifier.N49f237d27(i);
        }
        return p;
    }
    static double N366972ef6(Object []i) {
        double p = Double.NaN;
        if (i[7] == null) {
            p = 2;
        } else if (((Double) i[7]).doubleValue() <= 1.324894) {
            p = 2;
        } else if (((Double) i[7]).doubleValue() > 1.324894) {
            p = 1;
        }
        return p;
    }
    static double N49f237d27(Object []i) {
        double p = Double.NaN;
        if (i[7] == null) {
            p = 1;
        } else if (((Double) i[7]).doubleValue() <= 1.132956) {
            p = 1;
        } else if (((Double) i[7]).doubleValue() > 1.132956) {
            p = 0;
        }
        return p;
    }
    static double N4b1b51c68(Object []i) {
        double p = Double.NaN;
        if (i[0] == null) {
            p = 1;
        } else if (((Double) i[0]).doubleValue() <= 268.487292) {
            p = WekaClassifier.N48cede1f9(i);
        } else if (((Double) i[0]).doubleValue() > 268.487292) {
            p = 2;
        }
        return p;
    }
    static double N48cede1f9(Object []i) {
        double p = Double.NaN;
        if (i[7] == null) {
            p = 1;
        } else if (((Double) i[7]).doubleValue() <= 6.921534) {
            p = WekaClassifier.N354d23e310(i);
        } else if (((Double) i[7]).doubleValue() > 6.921534) {
            p = WekaClassifier.N3d585cc115(i);
        }
        return p;
    }
    static double N354d23e310(Object []i) {
        double p = Double.NaN;
        if (i[64] == null) {
            p = 1;
        } else if (((Double) i[64]).doubleValue() <= 5.393589) {
            p = 1;
        } else if (((Double) i[64]).doubleValue() > 5.393589) {
            p = WekaClassifier.N6d59ce3f11(i);
        }
        return p;
    }
    static double N6d59ce3f11(Object []i) {
        double p = Double.NaN;
        if (i[12] == null) {
            p = 1;
        } else if (((Double) i[12]).doubleValue() <= 0.702186) {
            p = 1;
        } else if (((Double) i[12]).doubleValue() > 0.702186) {
            p = WekaClassifier.N7cdd373312(i);
        }
        return p;
    }
    static double N7cdd373312(Object []i) {
        double p = Double.NaN;
        if (i[5] == null) {
            p = 2;
        } else if (((Double) i[5]).doubleValue() <= 3.214168) {
            p = 2;
        } else if (((Double) i[5]).doubleValue() > 3.214168) {
            p = WekaClassifier.N42dd209113(i);
        }
        return p;
    }
    static double N42dd209113(Object []i) {
        double p = Double.NaN;
        if (i[0] == null) {
            p = 1;
        } else if (((Double) i[0]).doubleValue() <= 189.108998) {
            p = WekaClassifier.N1ed3411214(i);
        } else if (((Double) i[0]).doubleValue() > 189.108998) {
            p = 2;
        }
        return p;
    }
    static double N1ed3411214(Object []i) {
        double p = Double.NaN;
        if (i[17] == null) {
            p = 2;
        } else if (((Double) i[17]).doubleValue() <= 0.500974) {
            p = 2;
        } else if (((Double) i[17]).doubleValue() > 0.500974) {
            p = 1;
        }
        return p;
    }
    static double N3d585cc115(Object []i) {
        double p = Double.NaN;
        if (i[0] == null) {
            p = 1;
        } else if (((Double) i[0]).doubleValue() <= 206.540439) {
            p = WekaClassifier.N28a980f616(i);
        } else if (((Double) i[0]).doubleValue() > 206.540439) {
            p = 2;
        }
        return p;
    }
    static double N28a980f616(Object []i) {
        double p = Double.NaN;
        if (i[64] == null) {
            p = 0;
        } else if (((Double) i[64]).doubleValue() <= 4.651428) {
            p = WekaClassifier.N3dbf4da17(i);
        } else if (((Double) i[64]).doubleValue() > 4.651428) {
            p = 1;
        }
        return p;
    }
    static double N3dbf4da17(Object []i) {
        double p = Double.NaN;
        if (i[7] == null) {
            p = 2;
        } else if (((Double) i[7]).doubleValue() <= 8.871604) {
            p = 2;
        } else if (((Double) i[7]).doubleValue() > 8.871604) {
            p = 0;
        }
        return p;
    }
}
