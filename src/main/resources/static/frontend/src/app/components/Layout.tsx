import { useState, useEffect, useRef } from "react";
import { Outlet, Link, useLocation, useNavigate } from "react-router";
import {
  MapIcon,
  ActivityIcon,
  Building2,
  Users,
  SearchIcon,
  BellIcon,
  MenuIcon,
  XIcon,
  LogOutIcon,
  KeyIcon,
  ChevronDownIcon,
} from "lucide-react";
import { motion, AnimatePresence } from "motion/react";
import { useAuth } from "../context/AuthContext";
import { useApi } from "../hooks/useApi";

export default function Layout() {
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout, login } = useAuth();
  const api = useApi();
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [profileDropdownOpen, setProfileDropdownOpen] = useState(false);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const dropdownRef = useRef<HTMLDivElement>(null);
  const notificationsRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    // Fetch latest user profile to ensure we have assignedFacilityId and approval status
    api.get("/api/users/me")
      .then(profile => {
        if (user) {
          login(localStorage.getItem('token')!, profile.role, profile.userId, profile.email);
        }
      })
      .catch(err => console.error("Failed to refresh profile", err));
  }, []);

  // Close dropdowns when clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setProfileDropdownOpen(false);
      }
      if (notificationsRef.current && !notificationsRef.current.contains(event.target as Node)) {
        setNotificationsOpen(false);
      }
    }

    if (profileDropdownOpen || notificationsOpen) {
      document.addEventListener("mousedown", handleClickOutside);
      return () => document.removeEventListener("mousedown", handleClickOutside);
    }
  }, [profileDropdownOpen, notificationsOpen]);

  const handleLogout = () => {
    api.post("/api/auth/logout").finally(() => {
      logout();
      navigate("/");
    });
  };

  const isActive = (path: string) => location.pathname === path;

  // Define navigation items based on role
  const getNavigationItems = () => {
    const baseItems = [
      { path: "/app/dashboard", icon: MapIcon, label: "Map View", roles: ["all"] },
      { path: "/app/districts", icon: Building2, label: "District Explorer", roles: ["all"] },
    ];

    const roleSpecificItems = [
      { path: "/app/simulations", icon: ActivityIcon, label: "Simulations", roles: ["MINISTRY_OFFICIAL"] },
      { path: "/app/my-facility", icon: Building2, label: "My Facility", roles: ["HOSPITAL_ADMIN"] },
    ];

    return [...baseItems, ...roleSpecificItems].filter(
      (item) => item.roles.includes("all") || item.roles.includes(user?.role || "")
    );
  };

  const navigationItems = getNavigationItems();

  const userName = user?.username || "User";
  const userRoleStr = user?.role || "PUBLIC";

  return (
    <div className="min-h-screen w-full bg-gray-50 flex">
      {/* Sidebar */}
      <aside
        className={`${
          sidebarOpen ? "w-64" : "w-20"
        } bg-white border-r border-gray-200 transition-all duration-300 flex flex-col fixed h-full z-20`}
      >
        {/* Logo Section */}
        <div className="h-16 border-b border-gray-200 flex items-center justify-between px-4">
          {sidebarOpen && (
            <div className="flex items-center gap-3">
              <div className="relative w-8 h-8">
                <svg viewBox="0 0 100 100" className="w-full h-full">
                  <path d="M 20 40 L 50 20 L 80 40 L 80 85 L 20 85 Z" fill="#8B3A3A" />
                  <rect x="35" y="45" width="30" height="8" fill="#7DD3FC" rx="2" />
                  <rect x="46" y="36" width="8" height="28" fill="#7DD3FC" rx="2" />
                </svg>
              </div>
              <span className="text-lg font-bold text-slate-900">LHIMS</span>
            </div>
          )}
          <button
            onClick={() => setSidebarOpen(!sidebarOpen)}
            className="p-2 hover:bg-gray-100 rounded-lg transition"
          >
            {sidebarOpen ? <XIcon size={20} /> : <MenuIcon size={20} />}
          </button>
        </div>

        {/* Navigation */}
        <nav className="flex-1 py-6 px-3 space-y-1 overflow-y-auto">
          {navigationItems.map((item) => {
            const Icon = item.icon;
            const active = isActive(item.path);
            return (
              <Link
                key={item.path}
                to={item.path}
                className={`flex items-center gap-3 px-3 py-3 rounded-lg transition group ${
                  active
                    ? "bg-[#8B3A3A] text-white"
                    : "text-gray-600 hover:bg-gray-100"
                }`}
              >
                <Icon size={20} className={active ? "text-white" : "text-gray-500 group-hover:text-gray-700"} />
                {sidebarOpen && <span className="font-medium">{item.label}</span>}
              </Link>
            );
          })}
        </nav>

        {/* User Info at Bottom */}
        {sidebarOpen && (
          <div className="p-4 border-t border-gray-200">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-gradient-to-br from-[#8B3A3A] to-[#7DD3FC] rounded-full flex items-center justify-center text-white font-semibold">
                {userName.charAt(0)}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-gray-900 truncate">{userName}</p>
                <p className="text-xs text-gray-500 truncate capitalize">
                  {userRoleStr.replace(/_/g, " ")}
                </p>
              </div>
            </div>
          </div>
        )}
      </aside>

      {/* Main Content Area */}
      <div className={`flex-1 flex flex-col ${sidebarOpen ? "ml-64" : "ml-20"} transition-all duration-300`}>
        {/* Top Header */}
        <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-6 sticky top-0 z-10">
          {/* Search Bar - Only on Dashboard */}
          {location.pathname === "/app/dashboard" && (
            <div className="flex-1 max-w-md">
              <div className="relative">
                <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Search districts, hospitals..."
                  className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#7DD3FC] focus:border-transparent"
                />
              </div>
            </div>
          )}
          {location.pathname !== "/app/dashboard" && <div className="flex-1"></div>}

          {/* Right Section */}
          <div className="flex items-center gap-4">
            {/* Notifications */}
            <div className="relative" ref={notificationsRef}>
              <button
                onClick={() => setNotificationsOpen(!notificationsOpen)}
                className="relative p-2 hover:bg-gray-100 rounded-lg transition"
              >
                <BellIcon size={20} className="text-gray-600" />
                <span className="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full"></span>
              </button>

              {/* Notifications Dropdown */}
              <AnimatePresence>
                {notificationsOpen && (
                  <motion.div
                    initial={{ opacity: 0, y: -10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -10 }}
                    className="absolute right-0 mt-2 w-80 bg-white rounded-lg shadow-lg border border-gray-200 z-50"
                  >
                    <div className="px-4 py-3 border-b border-gray-200">
                      <h3 className="font-semibold text-slate-900">Notifications</h3>
                    </div>
                    <div className="max-h-96 overflow-y-auto">
                      <div className="p-3 hover:bg-gray-50 border-b border-gray-100 cursor-pointer transition">
                        <div className="flex items-start gap-3">
                          <div className="w-2 h-2 bg-red-500 rounded-full mt-2"></div>
                          <div className="flex-1">
                            <p className="text-sm font-medium text-slate-900">Critical Alert: Akkar District</p>
                            <p className="text-xs text-gray-600 mt-1">CIRI score reached 85, immediate intervention recommended</p>
                            <p className="text-xs text-gray-500 mt-1">2 hours ago</p>
                          </div>
                        </div>
                      </div>
                      <div className="p-3 hover:bg-gray-50 border-b border-gray-100 cursor-pointer transition">
                        <div className="flex items-start gap-3">
                          <div className="w-2 h-2 bg-amber-500 rounded-full mt-2"></div>
                          <div className="flex-1">
                            <p className="text-sm font-medium text-slate-900">Simulation Completed</p>
                            <p className="text-xs text-gray-600 mt-1">Healthcare Expansion - Q2 2026 results are now available</p>
                            <p className="text-xs text-gray-500 mt-1">5 hours ago</p>
                          </div>
                        </div>
                      </div>
                      <div className="p-3 hover:bg-gray-50 border-b border-gray-100 cursor-pointer transition">
                        <div className="flex items-start gap-3">
                          <div className="w-2 h-2 bg-blue-500 rounded-full mt-2"></div>
                          <div className="flex-1">
                            <p className="text-sm font-medium text-slate-900">New Hospital Data Available</p>
                            <p className="text-xs text-gray-600 mt-1">Bekaa Valley Hospital capacity has been updated</p>
                            <p className="text-xs text-gray-500 mt-1">1 day ago</p>
                          </div>
                        </div>
                      </div>
                      <div className="p-3 hover:bg-gray-50 cursor-pointer transition">
                        <div className="flex items-start gap-3">
                          <div className="w-2 h-2 bg-green-500 rounded-full mt-2"></div>
                          <div className="flex-1">
                            <p className="text-sm font-medium text-slate-900">Monthly Report Generated</p>
                            <p className="text-xs text-gray-600 mt-1">March 2026 regional assessment report is ready for review</p>
                            <p className="text-xs text-gray-500 mt-1">2 days ago</p>
                          </div>
                        </div>
                      </div>
                    </div>
                    <div className="px-4 py-3 border-t border-gray-200 text-center">
                      <button className="text-sm text-[#8B3A3A] hover:text-[#6B2A2A] font-medium">
                        View All Notifications
                      </button>
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>
            </div>

            {/* Profile Dropdown */}
            <div className="relative" ref={dropdownRef}>
              <button
                onClick={() => setProfileDropdownOpen(!profileDropdownOpen)}
                className="flex items-center gap-2 hover:bg-gray-100 rounded-lg px-3 py-2 transition"
              >
                <div className="w-8 h-8 bg-gradient-to-br from-[#8B3A3A] to-[#7DD3FC] rounded-full flex items-center justify-center text-white text-sm font-semibold">
                  {userName.charAt(0)}
                </div>
                <ChevronDownIcon size={16} className="text-gray-600" />
              </button>

              {/* Dropdown Menu */}
              <AnimatePresence>
                {profileDropdownOpen && (
                  <motion.div
                    initial={{ opacity: 0, y: -10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -10 }}
                    className="absolute right-0 mt-2 w-56 bg-white rounded-lg shadow-lg border border-gray-200 py-2 z-50"
                  >
                    <div className="px-4 py-3 border-b border-gray-200">
                      <p className="text-sm font-medium text-gray-900">{userName}</p>
                      <p className="text-xs text-gray-500 capitalize mt-1">
                        {userRoleStr.replace(/_/g, " ")}
                      </p>
                    </div>
                    <button
                      onClick={() => {
                        setProfileDropdownOpen(false);
                        navigate("/app/change-password");
                      }}
                      className="w-full flex items-center gap-3 px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 transition"
                    >
                      <KeyIcon size={16} />
                      <span>Change Password</span>
                    </button>
                    <div className="border-t border-gray-200 pt-2">
                      <button
                        onClick={() => {
                          setProfileDropdownOpen(false);
                          handleLogout();
                        }}
                        className="w-full flex items-center gap-3 px-4 py-2 text-sm text-red-600 hover:bg-red-50 transition"
                      >
                        <LogOutIcon size={16} />
                        <span>Logout</span>
                      </button>
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
          </div>
        </header>

        {/* Main Content */}
        <main className="flex-1 p-6 overflow-y-auto bg-gray-50">
          <Outlet context={{ searchQuery }} />
        </main>
      </div>
    </div>
  );
}
