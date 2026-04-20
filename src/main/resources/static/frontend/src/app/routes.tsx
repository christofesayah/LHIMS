import { createBrowserRouter } from "react-router";
import Login from "./components/Login.tsx";
import Dashboard from "./components/Dashboard.tsx";
import RegionDetails from "./components/RegionDetails.tsx";
import Simulations from "./components/Simulations.tsx";
import DistrictExplorer from "./components/DistrictExplorer.tsx";
import MyFacility from "./components/MyFacility.tsx";
import ChangePassword from "./components/ChangePassword.tsx";
import Layout from "./components/Layout.tsx";
import { ProtectedRoute } from "./components/ProtectedRoute.tsx";

export const router = createBrowserRouter([
  {
    path: "/",
    Component: Login,
  },
  {
    path: "/app",
    element: <ProtectedRoute />,
    children: [
      {
        element: <Layout />,
        children: [
          {
            path: "dashboard",
            Component: Dashboard,
          },
          {
            path: "districts",
            Component: DistrictExplorer,
          },
          {
            path: "region/:regionId",
            Component: RegionDetails,
          },
          {
            element: <ProtectedRoute allowedRoles={["MINISTRY_OFFICIAL"]} />,
            children: [
              {
                path: "simulations",
                Component: Simulations,
              },
            ],
          },
          {
            element: <ProtectedRoute allowedRoles={["HOSPITAL_ADMIN"]} />,
            children: [
              {
                path: "my-facility",
                Component: MyFacility,
              },
            ],
          },
          {
            path: "change-password",
            Component: ChangePassword,
          },
        ],
      },
    ],
  },
]);
