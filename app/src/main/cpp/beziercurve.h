//
// Created by Kuraifune on 16.01.2023.
//

#ifndef RAW_CONVERTER_BEZIERCURVE_H
#define RAW_CONVERTER_BEZIERCURVE_H
#include <iostream>
#include <vector>
#include <tuple>
#include <cmath>

using namespace std;

//-----------------------------------------------------------

// https://cplusplus.com/forum/beginner/234717/
vector<double> cubicSolve(double a, double b, double c, double d) {
    b /= a;
    c /= a;
    d /= a;

    double disc, q, r, dum1, s, t, term1, r13;
    q = (3.0*c - (b*b))/9.0;
    r = -(27.0*d) + b*(9.0*c - 2.0*(b*b));
    r /= 54.0;
    disc = q*q*q + r*r;
    term1 = (b/3.0);

    double x1_real, x2_real, x3_real;
    double x2_imag, x3_imag;
    string x2_imag_s, x3_imag_s;
    if (disc > 0)   // One root real, two are complex
    {
        s = r + sqrt(disc);
        s = s<0 ? -cbrt(-s) : cbrt(s);
        t = r - sqrt(disc);
        t = t<0 ? -cbrt(-t) : cbrt(t);
        x1_real = -term1 + s + t;
        term1 += (s + t)/2.0;
        x3_real = x2_real = -term1;
        term1 = sqrt(3.0)*(-t + s)/2;
        x2_imag = term1;
        x3_imag = -term1;
        x2_imag_s =  " + "+ to_string(x2_imag) + "i";
        x3_imag_s =  " - "+ to_string(x2_imag) + "i";
    }
        // The remaining options are all real
    else if (disc == 0)  // All roots real, at least two are equal.
    {
        x3_imag = x2_imag = 0;
        r13 = r<0 ? -cbrt(-r) : cbrt(r);
        x1_real = -term1 + 2.0*r13;
        x3_real = x2_real = -(r13 + term1);
    }
        // Only option left is that all roots are real and unequal (to get here, q < 0)
    else
    {
        x3_imag = x2_imag = 0;
        q = -q;
        dum1 = q*q*q;
        dum1 = acos(r/sqrt(dum1));
        r13 = 2.0*sqrt(q);
        x1_real = -term1 + r13*cos(dum1/3.0);
        x2_real = -term1 + r13*cos((dum1 + 2.0*M_PI)/3.0);
        x3_real = -term1 + r13*cos((dum1 + 4.0*M_PI)/3.0);
    }

//    cout << "\nRoots:" << endl <<
//         "  x = " << x1_real << endl <<
//         "  x = " << x2_real << x2_imag_s << endl <<
//         "  x = " << x3_real << x3_imag_s << endl;

    vector<double> res{x1_real, x2_real, x3_real};

    return res;
}

// From https://medium.com/geekculture/2d-and-3d-b%C3%A9zier-curves-in-c-499093ef45a9
vector<double> computeBesierCurve2D(vector<double> xX, vector<double> yY, int start, int stop)
{

    vector<double> bCurveY;
    double bCurveYt;

    double a = -xX[0] + 3 * xX[1] - 3 * xX[2] + xX[3];
    double b = 3 * xX[0] - 6 * xX[1] + 3 * xX[2];
    double c = -3 * xX[0] + 3 * xX[1];
    double d = xX[0];

    vector<double> ts;

    for (int i = start; i < stop; i += 1)
    {
        ts = cubicSolve(a, b, c, d - (double) i);

        double t = ts[0];
        for (double j : ts) {
            if (j >=0 && j < 65536) {
                t = j;
                break;
            }
        }

        bCurveYt = pow((1 - t), 3) * yY[0] + 3 * pow((1 - t), 2) * t * yY[1] + 3 * pow((1 - t), 1) * pow(t, 2) * yY[2] + pow(t, 3) * yY[3];
        bCurveY.push_back(bCurveYt);
    }

    return bCurveY;
}

#endif //RAW_CONVERTER_BEZIERCURVE_H

