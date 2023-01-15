//// g++ bezier_curve.cpp -o t -I/usr/include/python3.8 -lpython3.8
//// From https://medium.com/geekculture/2d-and-3d-b%C3%A9zier-curves-in-c-499093ef45a9
#include <iostream>
#include <vector>
#include <tuple>
#include <cmath>

using namespace std;

//-----------------------------------------------------------

tuple<std::vector<double>, vector<double>> computeBesierCurve2D(vector<double> xX, vector<double> yY)
{

    vector<double> bCurveX;
    vector<double> bCurveY;
    double bCurveXt;
    double bCurveYt;

    for (double t = 0.01; t <= 1; t += 0.01)
    {

        bCurveXt = pow((1 - t), 3) * xX[0] + 3 * pow((1 - t), 2) * t * xX[1] + 3 * pow((1 - t), 1) * pow(t, 2) * xX[2] + pow(t, 3) * xX[3];
        bCurveYt = pow((1 - t), 3) * yY[0] + 3 * pow((1 - t), 2) * t * yY[1] + 3 * pow((1 - t), 1) * pow(t, 2) * yY[2] + pow(t, 3) * yY[3];

        bCurveX.push_back(bCurveXt);
        bCurveY.push_back(bCurveYt);
    }

    return make_tuple(bCurveX, bCurveY);
}

//-----------------------------------------------------------

int main()
{

    vector<double> xX{2.5, 1.5, 6, 10};
    vector<double> yY{0.5, 5, 5, 0.5};

    tuple<vector<double>, vector<double>> bCurve2D = computeBesierCurve2D(xX, yY);

}