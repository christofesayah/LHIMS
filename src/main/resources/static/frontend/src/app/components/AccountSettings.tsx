import { useState } from "react";
import { motion } from "motion/react";
import { UserIcon, MailIcon, BriefcaseIcon, SaveIcon } from "lucide-react";

export default function AccountSettings() {
  const [fullName, setFullName] = useState("Admin User");
  const [email, setEmail] = useState("admin@lhims.gov.lb");
  const [role, setRole] = useState("Ministry Official");
  const [isSaving, setIsSaving] = useState(false);
  const [savedSuccess, setSavedSuccess] = useState(false);

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);

    // Simulate API call
    setTimeout(() => {
      setIsSaving(false);
      setSavedSuccess(true);
      setTimeout(() => setSavedSuccess(false), 3000);
    }, 1000);
  };

  return (
    <div className="max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900 mb-2">
          Account Settings
        </h1>
        <p className="text-slate-600">
          Manage your account information and preferences
        </p>
      </div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="bg-white border border-gray-200 rounded-xl p-6 shadow-sm"
      >
        <div className="mb-6">
          <div className="flex items-center gap-4">
            <div className="w-20 h-20 bg-gradient-to-br from-[#8B3A3A] to-[#7DD3FC] rounded-full flex items-center justify-center text-white text-3xl font-semibold">
              {fullName.charAt(0)}
            </div>
            <div>
              <h2 className="text-xl font-semibold text-slate-900">{fullName}</h2>
              <p className="text-slate-600">{role}</p>
            </div>
          </div>
        </div>

        <form onSubmit={handleSave} className="space-y-5">
          <div>
            <label
              htmlFor="fullName"
              className="block text-sm font-medium text-slate-700 mb-2"
            >
              <div className="flex items-center gap-2">
                <UserIcon size={16} />
                Full Name
              </div>
            </label>
            <input
              id="fullName"
              type="text"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#7DD3FC] focus:border-transparent"
              required
            />
          </div>

          <div>
            <label
              htmlFor="email"
              className="block text-sm font-medium text-slate-700 mb-2"
            >
              <div className="flex items-center gap-2">
                <MailIcon size={16} />
                Email Address
              </div>
            </label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-[#7DD3FC] focus:border-transparent"
              required
            />
          </div>

          <div>
            <label
              htmlFor="role"
              className="block text-sm font-medium text-slate-700 mb-2"
            >
              <div className="flex items-center gap-2">
                <BriefcaseIcon size={16} />
                Role
              </div>
            </label>
            <input
              id="role"
              type="text"
              value={role}
              disabled
              className="w-full px-4 py-3 border border-gray-300 rounded-lg bg-gray-50 text-gray-600 cursor-not-allowed"
            />
            <p className="text-xs text-gray-500 mt-1">
              Contact an administrator to change your role
            </p>
          </div>

          <div className="pt-4 flex items-center gap-3">
            <motion.button
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              type="submit"
              disabled={isSaving}
              className="flex items-center gap-2 px-6 py-3 bg-[#8B3A3A] hover:bg-[#6B2A2A] disabled:bg-gray-400 text-white font-medium rounded-lg transition shadow-lg shadow-[#8B3A3A]/20"
            >
              <SaveIcon size={18} />
              {isSaving ? "Saving..." : "Save Changes"}
            </motion.button>

            {savedSuccess && (
              <motion.div
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                className="text-green-600 text-sm font-medium"
              >
                ✓ Changes saved successfully
              </motion.div>
            )}
          </div>
        </form>
      </motion.div>
    </div>
  );
}
