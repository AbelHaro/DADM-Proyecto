import React from 'react'

const EmailVerified = () => {
  return (
    <div className="min-h-screen bg-gray-100 flex items-center justify-center">
      <div className="bg-white p-8 rounded-lg shadow-md max-w-md w-full text-center">
        <div className="flex flex-col items-center justify-center space-y-4">
          <svg 
            className="mx-auto h-12 w-12 text-green-500 mb-4" 
            fill="none" 
            stroke="currentColor" 
            viewBox="0 0 24 24" 
            xmlns="http://www.w3.org/2000/svg"
          >
            <path 
              strokeLinecap="round" 
              strokeLinejoin="round" 
              strokeWidth={2} 
              d="M5 13l4 4L19 7" 
            />
          </svg>
          <h2 className="text-2xl font-bold text-gray-900">
            Email Verified!
          </h2>
          <p className="text-gray-600 w-full">
            Your email address has been successfully verified. You can now start using your account.
          </p>
          <a
            href="/"
            className="inline-block bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 px-4 rounded-md transition duration-150 ease-in-out"
          >
            Back to Home
          </a>
        </div>
      </div>
    </div>
  )
}

export default EmailVerified