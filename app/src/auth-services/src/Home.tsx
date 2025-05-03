import React from 'react'

const Home = () => {
  return (
    <div className="min-h-screen bg-gray-100 flex items-center justify-center">
      <div className="bg-white p-8 rounded-lg shadow-md max-w-md w-full text-center">
        <div className="flex flex-col items-center justify-center space-y-4">
          <h2 className="text-2xl font-bold text-gray-900">
            DADM Project
          </h2>
          <p className="text-gray-600 w-full">
            This is a demo page for the DADM project (Mobile Application Development)
          </p>
          <p className="text-sm text-gray-500">
            Polytechnic University of Valencia
          </p>
        </div>
      </div>
    </div>
  )
}

export default Home